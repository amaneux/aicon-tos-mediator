package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.cdc.CDCConfig;
import com.aicon.tos.connect.cdc.CDCData;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import com.aicon.tos.connect.http.HttpConnector;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.exceptions.DeserializationException;
import com.aicon.tos.shared.exceptions.SoapResponseTransformationException;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusProducer;
import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.util.TimeSyncService;
import com.avlino.common.utils.StringUtils;
import generated.AiconTosCanaryRequest;
import generated.AiconTosCanaryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.aicon.tos.shared.util.GregorianCalendarUtil.getXMLGregorianCalender;
import static com.aicon.tos.shared.xml.XmlUtil.deserialize;
import static com.aicon.tos.shared.xml.XmlUtil.serialize;

public class CanarySession extends FlowSession {
    private String lastUsedRequestId;

    private static final Logger LOG = LoggerFactory.getLogger(CanarySession.class);

    private final CDCDataProcessor cdcProcessor;
    private final RequestResponseTransformer transformer;
    private final CDCController cdcController;
    private final DataStore dataStore = DataStore.getInstance();
    private final String n4Scope;
    private final boolean cdcCheck;
    private final CanaryController canaryController;

    private AiconTosCanaryResponse response;
    private long requestSequence = 0;
    private AiconTosConnectionStatusProducer aiconTosConnectionStatusProducer;

    public CanarySession(CanaryController canaryController,
                         CDCDataProcessor cdcProcessor,
                         AiconTosConnectionStatusProducer aiconTosConnectionStatusProducer,
                         ConfigGroup httpConfig,
                         RequestResponseTransformer transformer,
                         String n4Scope,
                         boolean cdcCheck) {
        super(canaryController, "CANARY_SESSION", httpConfig, null, null);
        this.canaryController = canaryController;
        this.cdcProcessor = cdcProcessor;
        this.aiconTosConnectionStatusProducer = aiconTosConnectionStatusProducer;
        this.transformer = transformer;
        this.cdcController = canaryController.getCDCController();
        this.n4Scope = n4Scope;
        this.cdcCheck = cdcCheck;
        this.setSessionType("CANARY_SESSION");

        LOG.info("Instantiate Canary session");
    }

    /**
     * Defines the unit of work to be executed by this FlowSession.
     * All subclass-specific logic should be implemented here.
     *
     * @throws FlowSessionException if execution fails
     */
    protected void execute() throws FlowSessionException {
        String requestId = String.valueOf(Instant.now().getEpochSecond());
        HttpConnector connector = getHttpConnector();

        try {
            AiconTosCanaryRequest request = buildRequest(requestId);
            String requestBody = transformer.transformRequest(serialize(request, AiconTosCanaryRequest.class), n4Scope);

            String rawResponse = connector.sendHttpRequest(httpConfig, requestBody);
            LOG.info("[Canary] Raw response:\n {}", rawResponse);

            if (rawResponse.contains("<soapenv:Fault") || rawResponse.contains("<faultcode>")) {
                String faultString = extractFaultString(rawResponse);
                LOG.error("[Canary] SOAP Fault detected: {}", faultString);
                throw new SoapResponseTransformationException("SOAP Fault in response: " + faultString);
            }

            String transformedResponse = transformer.transformResponse(rawResponse);
            response = deserializeResponse(transformedResponse);
            handleTimestamps(response);

            if (cdcCheck) handleCDC();
            this.lastUsedRequestId = requestId;
            processResponse();
        } catch (Exception e) {
            setState(SessionState.FAILED);
            setSessionFailureReason(StringUtils.concat(e.getClass().getSimpleName(), ": ", e.getMessage()));
            throw new FlowSessionException("CanarySession failed", e);
        } finally {
            sendConnectionStatusMessage(requestId);
        }
    }

    protected AiconTosCanaryResponse deserializeResponse(String transformedResponse) {
        try {
            // Attempt to deserialize the response
            return deserialize(LOG, transformedResponse, AiconTosCanaryResponse.class);
        } catch (DeserializationException e) {
            // Log the error and handle the exception cleanly
            LOG.error("Failed to deserialize the response: {}, {}", transformedResponse, e.getMessage());

            // Return a null or default object to represent failure
            // You can customize this part based on your requirements
            return null;
        } catch (Exception e) {
            // Handle any unexpected exceptions that might occur
            LOG.error("An unexpected exception occurred while deserializing: {}, {}", transformedResponse, e.getMessage());
            throw e; // Optionally rethrow if required
        }
    }

    private AiconTosCanaryRequest buildRequest(String requestId) {
        long currentMillis = Instant.now().toEpochMilli();
        AiconTosCanaryRequest request = new AiconTosCanaryRequest();
        request.setRequestId(requestId);
        request.setRequestSendTs(getXMLGregorianCalender(currentMillis));
        request.setRequestSendTsMillis(currentMillis);
        request.setSequence(requestSequence++);
        request.setCdcTables(cdcCheck ? cdcProcessor.getCDCTableNames() : "");
        return request;
    }

    private void handleTimestamps(AiconTosCanaryResponse response) {
        long now = Instant.now().toEpochMilli();
        response.setResponseReceivedTs(getXMLGregorianCalender(now));
        response.setResponseReceivedTsMillis(now);
    }

    private void handleCDC() {
        String rawCDC = response.getLatestCdcCreations();

        if (!cdcProcessor.expectedTOSCDCDataForConfiguredTables(rawCDC)) {
            String expectedTables = cdcProcessor.getCDCTableNames();
            LOG.error("Unexpected TOS CDC data: {}, expected: {}", rawCDC, expectedTables);
            return;
        }

        String prettyCDC = cdcProcessor.convertRawStringToPrettyCDCString(rawCDC);
        LOG.info("TOS CDC Data (unfiltered): {}", prettyCDC);

        canaryController.setLatestTOSCDCData(cdcProcessor.removeTooOldData(rawCDC));
    }

    private void processResponse() {
        determineTimeSync();
        if (cdcCheck) {
            List<CDCData> aiconCDC = getAiconCDCData();
            canaryController.setLatestAiconCDCData(aiconCDC);
            canaryController.setCdcOk(cdcProcessor.determineCdcStatus(
                    canaryController.getLatestTOSCDCData(), aiconCDC));
        }
    }

    private List<CDCData> getAiconCDCData() {
        List<CDCData> rawCDC = cdcProcessor.convertStringToCDCData(canaryController.getLatestTOSCDCData());
        rawCDC = cdcProcessor.removeTooOldData(rawCDC);
        List<CDCData> result = new ArrayList<>();
        for (CDCData entry : rawCDC) {
            CDCConfig config = cdcProcessor.getCdcConfigForTable(entry.getTableName());
            Long ts = cdcController.findDateForGKey(config, entry.getGkey(), config.threshold(), dataStore.getTimeSync());
            result.add(new CDCData(entry.getTableName(), entry.getGkey(), ts));
        }
        return result;
    }

    private void determineTimeSync() {
        try {
            Long sync = TimeSyncService.determineSyncTime(
                    response.getRequestSendTsMillis(),
                    response.getRequestReceivedTsMillis(),
                    response.getResponseSendTsMillis(),
                    response.getResponseReceivedTsMillis()
            );
            LOG.info("Time sync = {}", sync);
            canaryController.setTimeSync(sync);
        } catch (IllegalArgumentException e) {
            LOG.error("Missing timestamp(s) for time sync calculation", e);
        }
    }

    protected void sendConnectionStatusMessage(String requestId) {
        AiconTosConnectionStatusMessage msg = new AiconTosConnectionStatusMessage();
        msg.setRequestId(response != null ? response.getRequestId() : requestId);
        msg.setConnectionStatus(getState() == SessionState.DONE ? ConnectionStatus.OK : ConnectionStatus.NOK);
        msg.setCdcOk(getState() == SessionState.DONE && cdcCheck ? canaryController.getCdcOk() : Boolean.FALSE);
        try {
            aiconTosConnectionStatusProducer.sendMessage(msg.getRequestId().toString(), msg);
        } catch (Exception e) {
            setSessionFailureReason(e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    private String extractFaultString(String xml) {
        try {
            int start = xml.indexOf("<faultstring>");
            int end = xml.indexOf("</faultstring>");
            if (start != -1 && end != -1) {
                return xml.substring(start + "<faultstring>".length(), end).trim();
            }
        } catch (Exception e) {
            LOG.warn("Failed to extract faultstring", e);
        }
        return "Unknown SOAP fault";
    }

    protected HttpConnector getHttpConnector() {
        return new HttpConnector();
    }

    protected String getThreadName(int threadCounter) {
        return String.format("CanarySession-%d", threadCounter);
    }
}
