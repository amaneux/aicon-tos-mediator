package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestHelper;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateResponseProducer;
import com.aicon.tos.shared.schema.*;
import com.aicon.tos.shared.util.GregorianCalendarUtil;
import generated.AiconYardDeckingUpdateRequest;
import generated.AiconYardDeckingUpdateResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.aicon.tos.shared.util.TimeUtils.convertToInstant;
import static com.aicon.tos.shared.util.TimeUtils.convertToXMLGregorianCalendar;
import static com.aicon.tos.shared.xml.XmlUtil.prettyPrintXml;

/**
 * FlowSession implementation for decking update operations.
 * <p>
 * This session sends a transformed decking update request to an external HTTP endpoint
 * and logs the response. Uses HttpFlowSession to manage HTTP connector and transformation logic.
 */
public class DeckingUpdateFlowSession extends HttpFlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(DeckingUpdateFlowSession.class);
    private final AiconYardDeckingUpdateRequestMessage deckingUpdateRequestMessage;
    private boolean mockTOScall = false;

    public DeckingUpdateFlowSession(FlowController flowController,
                                    ConsumerRecord<String, AiconYardDeckingUpdateRequestMessage> deckingUpdateRequestMessage,
                                    ConfigGroup flowConfig, ConfigGroup httpConfig,
                                    RequestResponseTransformer transformer, String n4Scope) {
        super(flowController, "DECKING_UPDATE_FLOW", httpConfig, flowConfig, null, transformer, n4Scope);
        this.deckingUpdateRequestMessage = deckingUpdateRequestMessage.value();
        LOG.info("Instantiate DeckingUpdate FLOW session");
    }

    @Override
    public void execute() throws FlowSessionException {
        LOG.info("deckingUpdateRequestMessage: Id={}", deckingUpdateRequestMessage.getRequestId());

        AiconYardDeckingUpdateResponseMessage transformedResponse;
        try {
            if (!mockTOScall) {
                // Step 1: Transform the incoming message into a request object
                AiconYardDeckingUpdateRequest request = transformMessageToRequest(deckingUpdateRequestMessage);

                // Step 2: Send the transformed request and capture the raw response
                String rawResponse = sendHttpRequest(request, AiconYardDeckingUpdateRequest.class);

                if (LOG.isDebugEnabled()) {
                    LOG.debug("Raw DeckingUpdateResponse: \n{}", prettyPrintXml(rawResponse));
                }

                // Step 3: Transform the response into a final structure for processing
                AiconYardDeckingUpdateResponse response = deserializeResponse(rawResponse, AiconYardDeckingUpdateResponse.class);

                transformedResponse = transformResponseToMessage(response);
            } else {
                transformedResponse = getMockedResponse("" + deckingUpdateRequestMessage.getRequestId());
            }
            // Step 4: Send the transformed response
            AiconYardDeckingUpdateResponseProducer producer = new AiconYardDeckingUpdateResponseProducer();
            producer.sendMessage(deckingUpdateRequestMessage.getRequestId().toString(), transformedResponse);

        } catch (Exception e) {
            throw new FlowSessionException(getClass().getSimpleName() + " execution failed", e);
        }
    }

    AiconYardDeckingUpdateRequest transformMessageToRequest(
            AiconYardDeckingUpdateRequestMessage fromReq) {
        AiconYardDeckingUpdateRequest destReq = AiconYardDeckingUpdateRequestHelper.createWithDefaults();
        destReq.setRequestId(fromReq.getRequestId().toString());
        destReq.setTimeStamp(System.currentTimeMillis());
        destReq.setTimeStampLocalTimeISO8601(GregorianCalendarUtil.getNowInXMLGregorianCalender());

        for (DeckMove deckMove : fromReq.getMoves()) {
            AiconYardDeckingUpdateRequest.Moves.DeckMove deckMoveRequest = new AiconYardDeckingUpdateRequest.Moves.DeckMove();
            deckMoveRequest.setWiGkey(deckMove.getWiGkey().toString());
            deckMoveRequest.setPositionTo(deckMove.getPositionTo().toString());
            destReq.getMoves().getDeckMove().add(deckMoveRequest);
        }
        if (fromReq.getValidations() != null) {
            for (Condition cond : fromReq.getValidations()) {
                AiconYardDeckingUpdateRequest.Validations.Condition condReq = new AiconYardDeckingUpdateRequest.Validations.Condition();
                condReq.setField(cond.getField().toString());
                condReq.setValue(cond.getValue().toString());
                if (destReq.getValidations() == null) {
                    destReq.setValidations(new AiconYardDeckingUpdateRequest.Validations());
                }
                destReq.getValidations().getCondition().add(condReq);
            }
        }

        return destReq;
    }

    public AiconYardDeckingUpdateResponseMessage transformResponseToMessage(
            AiconYardDeckingUpdateResponse response) {

        AiconYardDeckingUpdateResponseMessage responseMessage;
        try {
            responseMessage = new AiconYardDeckingUpdateResponseMessage();
        } catch (Exception e) {
            throw new RuntimeException("Error while constructing AiconYardDeckingUpdateResponseMessage", e);
        }

        // Ensure requestId is properly set
        if (response.getRequestId() == null) {
            throw new IllegalArgumentException("requestId cannot be null in AiconYardDeckingUpdateResponse");
        }

        // Set top-level fields
        responseMessage.setRequestId(response.getRequestId());

        // Set mandatory fields with a fallback for null values
        responseMessage.setTimeStamp(
                Optional.of(response.getTimeStamp()).orElse(System.currentTimeMillis())
        );

        // Default to the current ISO8601 timestamp if null
        responseMessage.setTimeStampLocalTimeISO8601(convertToInstant(response.getTimeStampLocalTimeISO8601()));
        if(responseMessage.getResult() == null) {
            responseMessage.setResult(new Result());
        }
        responseMessage.getResult().setResultCode(response.getResultCode());
        responseMessage.getResult().setResultText(response.getResultText());
        responseMessage.getResult().setResultAudience(response.getResultAudience());
        responseMessage.getResult().setResultLevel(response.getResultLevel());

        // Handle single <moves> element with multiple <deckMove> entries
        List<DeckMove> deckMoves = new ArrayList<>();
        if (response.getMoves() != null && response.getMoves().getDeckMove() != null) {
            for (AiconYardDeckingUpdateResponse.Moves.DeckMove move : response.getMoves().getDeckMove()) {
                DeckMove deckMove = new DeckMove();
                deckMove.setResult(new Result());
                deckMove.setWiGkey(move.getWiGkey());
                deckMove.getResult().setResultCode(move.getResultCode());
                deckMove.getResult().setResultLevel(move.getResultLevel());
                deckMove.getResult().setResultText(move.getResultText());
                deckMove.getResult().setResultAudience(move.getResultAudience());
                deckMoves.add(deckMove);
            }
        }
        responseMessage.setMoves(deckMoves);

        return responseMessage;
    }

    @Override
    protected String getThreadName(int threadCounter) {
        return String.format("%s-%s", flowConfig.getName(), threadCounter);
    }

    private AiconYardDeckingUpdateResponseMessage getMockedResponse(String requestWiGKey) {
        AiconYardDeckingUpdateResponseMessage mockedResponse = new AiconYardDeckingUpdateResponseMessage();
        //TODO
//        mockedResponse.setWiGkey(requestWiGKey);
//        mockedResponse.setResultCode(String.format("SUCCESS for %s", requestWiGKey));
//        mockedResponse.setResultText("Mocked response for testing purposes.");
//        mockedResponse.setResultAudience("TheAudience");
//        mockedResponse.setResultLevel("LEVEL-UP");
        return mockedResponse;
    }
}
