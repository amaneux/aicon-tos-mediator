package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.connect.cdc.CDCData;
import com.aicon.tos.shared.exceptions.DeserializationException;
import com.aicon.tos.shared.exceptions.SerializationException;
import generated.AiconTosCanaryRequest;
import generated.AiconTosCanaryResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.aicon.tos.shared.xml.XmlUtil.deserialize;
import static com.aicon.tos.shared.xml.XmlUtil.serialize;

public class CananyMockupWebService extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(CananyMockupWebService.class);
    private static final List<WebServiceListener> listeners = new ArrayList<>();
    private static final AtomicReference<AiconTosCanaryResponse> modifiedResponse = new AtomicReference<>();

    private static boolean blockResponseSend = false;

    public static void registerListener(WebServiceListener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(WebServiceListener listener) {
        listeners.remove(listener);
    }

    public static void setBlockResponseSend(boolean block) {
        blockResponseSend = block;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.info(">>>>>>>> POST request received at /CananyMockupWebService");

        try {
            // Read and log the request body
            String requestBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            LOG.info("Request Body: {}", requestBody);

            // Deserialize the request
            AiconTosCanaryRequest request = deserialize(LOG, requestBody, AiconTosCanaryRequest.class);

            // Notify listeners
            for (WebServiceListener listener : listeners) {
                listener.onRequestReceived(request);
            }

            // Create or use the modified response
            if (getModifiedResponse() == null) {
                setModifiedResponse(createResponse(request));
            }

            // Block until response is ready
            synchronized (this) {
                while (blockResponseSend) {
                    this.wait(1000);
                }
            }

            // Serialize and send the response
            sendResponse(resp, getModifiedResponse());

        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request processing was interrupted", e);
        }
        catch (DeserializationException e) {
            LOG.error("Deserialization request error", e);
        }
    }


    public static AiconTosCanaryResponse createResponse(AiconTosCanaryRequest request) {
        AiconTosCanaryResponse response = new AiconTosCanaryResponse();
        response.setRequestId(request.getRequestId());
        response.setSequence(request.getSequence());

        long currentMillis = Instant.now().toEpochMilli();
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(currentMillis);
        XMLGregorianCalendar xmlGregorianCalendar;
        try {
            xmlGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
        response.setRequestSendTs(request.getRequestSendTs());
        response.setRequestSendTsMillis(request.getRequestSendTsMillis());

        response.setRequestReceivedTs(xmlGregorianCalendar);
        response.setRequestReceivedTsMillis(gregorianCalendar.getTimeInMillis());

        response.setResponseSendTs(xmlGregorianCalendar);
        response.setResponseSendTsMillis(gregorianCalendar.getTimeInMillis());

        response.setLatestCdcCreations(getcdcTableInfo(request));
        return response;
    }

    private void sendResponse(HttpServletResponse resp, Object response) throws IOException {
        try {
            String xmlResponse = serialize(response, AiconTosCanaryRequest.class);
            resp.setContentType("application/xml");
            resp.setCharacterEncoding("UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);

            try (PrintWriter out = resp.getWriter()) {
                out.write(xmlResponse);
            }
        }
        catch (SerializationException e) {
            LOG.error("Error serializing response to XML", e);
        }
    }

    private void handleException(HttpServletResponse resp, int statusCode, String errorMessage, Exception e)
            throws IOException {
        LOG.error(errorMessage, e);
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", errorMessage));
    }

    private static String getcdcTableInfo(AiconTosCanaryRequest request) {

        // format:   table=key:timestamp;table=key:timestamp;table=key:timestamp
        List<String> tables = List.of(request.getCdcTables().split(","));
        CDCData cdcData;
        StringBuilder bld = new StringBuilder();
        for (String table : tables) {
            cdcData = CDCData.createRandomValues(table);
            bld.append(cdcData.toXMLString()).append(";");
        }
        if (!bld.isEmpty()) {
            bld.setLength(bld.length() - 1);
        }
        return bld.toString();
    }

    public static synchronized void setModifiedResponse(AiconTosCanaryResponse response) {
        modifiedResponse.set(response);
    }

    public static synchronized AiconTosCanaryResponse getModifiedResponse() {
        return modifiedResponse.get();
    }


    public interface WebServiceListener {
        void onRequestReceived(AiconTosCanaryRequest requestBody);
    }
}
