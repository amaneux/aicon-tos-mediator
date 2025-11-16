package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.model.MoveInfo;
import com.aicon.tos.interceptor.InterceptorEntityConfig;
import com.aicon.tos.interceptor.MessageMeta;
import com.aicon.tos.interceptor.decide.AiconDeckingEngineResponse;
import com.aicon.tos.interceptor.decide.ResponseManager;
import com.aicon.tos.interceptor.decide.scenarios.ScenarioBase;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import com.aicon.tos.interceptor.decide.scenarios.n4.messages.TosDeckingUpdateResponse;
import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingRequestProducer;
import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingResponseConsumer;
import com.aicon.tos.interceptor.newgenproducerconsumer.messages.AiconDeckingRequest;
import com.aicon.tos.interceptor.newgenproducerconsumer.messages.AiconMessage;
import com.aicon.tos.model.PositionConverter;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestMessageHelper;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestProducer;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateResponseConsumer;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import com.aicon.tos.shared.schema.Condition;
import com.aicon.tos.shared.schema.DeckMove;
import com.aicon.tos.shared.util.AnsiColor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader.getKeySchemaFromRegistry;

/**
 * Base class for any scenario with some common functionality.
 */
abstract public class DeckingScenarioBase extends ScenarioBase {
    public static final int AICON_RESPONSE_TIMEOUT_S        = 20;
    public static final int TOS_RESPONSE_TIMEOUT_S          = 20;
    // this can be SHADOW mode or some malfunction in the decking_engine; anyway, this should not progress to the TOS
    public static final boolean STOP_WHEN_SAME_POS          = true;

    // These values can be overridden by the specific implementations of this base class.
    protected int aiconResponseTimeoutS = AICON_RESPONSE_TIMEOUT_S;
    protected int tosResponseTimeoutS = TOS_RESPONSE_TIMEOUT_S;

    protected AiconDeckingRequestProducer aiconDeckingRequestProducer = null;
    protected ResponseManager<GenericRecord> aiconDeckingResponseManager = null;

    protected AiconYardDeckingUpdateRequestProducer tosDeckingRequestProducer = null;
    protected ResponseManager<GenericRecord> tosDeckingResponseManager = null;

    public void init(String name, InterceptorEntityConfig entityName) {
        super.init(name, entityName);
        initConnectors();
    }

    /**
     * Initializes the consumers when not initialized yet.
     * @return true when all consumers are ready, else recall later to try again.
     */
    protected boolean initConnectors() {
        if (running) {
            return true;
        }
        try {
            if (aiconDeckingRequestProducer == null) {
                aiconDeckingRequestProducer = new AiconDeckingRequestProducer();
            }
            if (aiconDeckingResponseManager == null) {
                aiconDeckingResponseManager = new ResponseManager<>(new AiconDeckingResponseConsumer(config.getGroupId(), false));
            }
            if (tosDeckingRequestProducer == null) {
                tosDeckingRequestProducer = new AiconYardDeckingUpdateRequestProducer();
            }
            if (tosDeckingResponseManager == null) {
                tosDeckingResponseManager = new ResponseManager<>(new AiconYardDeckingUpdateResponseConsumer(config.getGroupId()));
            }
            running = true;
        } catch (Exception e) {
            running = false;
        }

        if (isRunning()) {
            getLogger().info(AnsiColor.blue("Connectors successfully started"));
            return true;
        } else {
            getLogger().error(AnsiColor.red("Failed connecting. Check VPN, kafka bootstrap servers"));
            return false;
        }
    }

    @Override
    public boolean isRunning() {
        boolean run1 = aiconDeckingResponseManager != null && aiconDeckingResponseManager.getConsumer().getStatus().getState().isRunning();
        boolean run2 =   tosDeckingResponseManager != null &&   tosDeckingResponseManager.getConsumer().getStatus().getState().isRunning();
        return super.isRunning() && run1 && run2;
    }

    /**
     * Sends a decking message by processing the input messages, interacting with the Aicon Decking Engine,
     * and dispatching requests to the TOS system with appropriate updates and responses.
     *
     * @param msgMeta meta of the filtered message, used to collect metrics about the progress of processing.
     * @param wiList the list of WorkInstructions
     * @param requestedBlockOnly when true, decking-engine will only deck in current block
     * @param tosConditions provides a list of conditions the TOS should use to validate the request
     */
    protected void sendDeckingMessageToAiconAndTOS(
            MessageMeta msgMeta,
            List<WorkInstructionEvent> wiList,
            boolean requestedBlockOnly,
            List<Condition> tosConditions
    ) {
        if (!initConnectors()) {     // check if all consumers are ready, else no use to process this message (gets lost).
            return;
        }
        AiconDeckingEngineResponse aiconDeckingResponse = callAiconDeckingEngine(msgMeta, wiList, requestedBlockOnly);

        if (aiconDeckingResponse != null) {
            callTosDeckingUpdate(msgMeta, aiconDeckingResponse, tosConditions);
        }
    }

    /**
     * Sends a aicon decking request to the request topic of
     * @param msgMeta the metadata of the CDC message
     * @param wiList the WI list to send for
     * @return a response from the decking engine or null when something has failed (expect enough logging to be done already).
     */
    protected AiconDeckingEngineResponse callAiconDeckingEngine(
            MessageMeta msgMeta,
            List<WorkInstructionEvent> wiList,
            boolean requestedBlockOnly
    ) {
        String uniqueKey = createUniqueId();
        String requestTopic = aiconDeckingRequestProducer.getTopicName();
        String responseTopic = aiconDeckingResponseManager.getTopicName();
        CompletableFuture<GenericRecord> aiconDeckingFuture = aiconDeckingResponseManager.registerAndGetFuture(uniqueKey)
                .orTimeout(aiconResponseTimeoutS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                            "Timeout({}s)/error when sending to {} for key {}, reason: {}",
                            aiconResponseTimeoutS, requestTopic, uniqueKey, ex);
                    return null;
                });

        Schema keySchema = getKeySchemaFromRegistry(requestTopic);

        List<Map<String, Object>> requests = new ArrayList<>();
        Map<Long, Map<String, String>> posMap = new HashMap<>();

        int idx = 0;
        for (WorkInstructionEvent wiEvent : wiList) {
            posMap.put(wiEvent.getGkey(), PositionConverter.convertPosPartsToAicon(PositionConverter.splitPosition(wiEvent.getPosSlot())));
            Map<String, Object> wiCtrInfo = MoveInfo.readMoveInfo(wiEvent.getGkey(), msgMeta);
            Map<String, Object> reqElm = AiconDeckingRequest.createRequestElement(
                    wiEvent.getGkey(),
                    wiEvent.getMoveKind().getJobType(),
                    getUptUserId(),
                    getProgramId(),
                    wiCtrInfo,
                    posMap.get(wiEvent.getGkey()),
                    wiEvent.getPosSlot(),
                    requestedBlockOnly
            );
            requests.add(reqElm);
            msgMeta.setEntityValues(idx++, "WI:{}:{}:{}:{}:{}",
                    wiEvent.getMoveKind().getTosCode(), wiEvent.getMoveNumber(), wiEvent.getGkey(), wiCtrInfo.get(MoveInfo.FLD_CTR_ID), wiEvent.getPosSlot());
        }
        GenericRecord aiconDeckingRequest = AiconDeckingRequest.createMessage(requestTopic, uniqueKey, requests);
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("New request for decking engine {}", AiconMessage.prettyPrintGenericRecord(aiconDeckingRequest));
        }

        // Send decking request to Aicon Decking Engine and wait for the response

        GenericRecord keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("request_index", uniqueKey);

        ProducerRecord<GenericRecord, GenericRecord> producerRecord = new ProducerRecord<>(
                aiconDeckingRequestProducer.getTopicName(),
                keyRecord,
                aiconDeckingRequest
        );

        try {
            // SEND TO TOPIC
            msgMeta.addTimestampWithPrefix(MessageMeta.TS_SEND_PREFIX, requestTopic, getLogger());
            aiconDeckingRequestProducer.sendMessage(producerRecord, (metadata, exc) -> {
                if (exc != null) {
                    msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                            "Failed to send {} for key {}, reason: {}",
                            requestTopic, uniqueKey, exc.getMessage());
                } else {
                    msgMeta.setResultWhenHigher(getLogger(), ResultLevel.OK,
                            "Successfully sent message with key {} to topic {} at offset {}."
                            , uniqueKey, requestTopic, metadata.offset());
                }
            });
        } catch (Exception e) {
                msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                        "Failed to send {} for key {}, reason: {}"
                        , requestTopic, uniqueKey, e.getMessage());
        }

        if (msgMeta.getResult() != null && msgMeta.getResult().getLevel() == ResultLevel.ERROR) {
            return null;
        }

        try {
            // WAIT FOR RESPONSE
            GenericRecord matchedResponse = aiconDeckingFuture.get();

            AiconDeckingEngineResponse aiconResponse = null;
            if (matchedResponse != null) {
                aiconResponse = AiconDeckingEngineResponse.fromGenericRecord(matchedResponse);
                msgMeta.addTimestampWithPrefix(MessageMeta.TS_RECV_PREFIX, responseTopic, getLogger());
                getLogger().info(AnsiColor.blue("Decking-engine response matched for {}: {}."), uniqueKey, matchedResponse);

                if (ResultLevel.OK != aiconResponse.getResult().getLevel()) {
                    msgMeta.setResultWhenHigher(getLogger(), aiconResponse.getResult());
                    getLogger().error("Response of decking-engine has 1 or more errors ({}), scenario ends.", aiconResponse.getResult());
                } else if (STOP_WHEN_SAME_POS) {
                    // verify if we have to stop if position did not change (SHADOW mode of decking engine).
                    boolean eq = true;
                    for (AiconDeckingEngineResponse.Request responseReq : aiconResponse.getRequests()) {
                        long respWiGkey = Long.valueOf(responseReq.getInternalIndex1());
                        Map<String, String> reqMap = posMap.get(respWiGkey);
                        if (reqMap != null) {
                            eq &= responseReq.getBlockIndexNumber().equals(reqMap.get(PositionConverter.POS_PART_BLOCK));
                            eq &= responseReq.getBayIndexNumber().equals(reqMap.get(PositionConverter.POS_PART_ROW));
                            eq &= responseReq.getRowIndexNumber().equals(reqMap.get(PositionConverter.POS_PART_COLUMN));
                            eq &= responseReq.getTierIndexNumber().equals(reqMap.get(PositionConverter.POS_PART_TIER));
                            if (eq) {
                                msgMeta.setResultWhenHigher(getLogger(), ResultLevel.WARN,
                                        "Response has same position as request, SHADOW mode detected and not decking to TOS");
                                aiconResponse = null;
                            }
                        }
                    }
                }
            } else {
                msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                        "Timeout({}s)/error when waiting on {} for matching response with key: {}.",
                        aiconResponseTimeoutS, responseTopic, uniqueKey);
            }
            return aiconResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                    "Interrupted while waiting for {} with key {}: {}",
                    responseTopic, uniqueKey, e.getMessage());
            return null;
        } catch (KafkaException e) {
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR, e.getMessage());
            return null;
        } catch (Exception e) {
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                    "Unexpected error while waiting for {} with key {}, reason: {}",
                    responseTopic, uniqueKey, e.getMessage());
            return null;
        }
    }

    /**
     * Calls the TOS via sending a request to the TOS decking request topic and waiting for its response in the TOS
     * response topic.
     * @param msgMeta the metadata of the CDC message
     * @param aiconDeckingResponse the response form the aicon decking engine when valid to be passed to TOS.
     */
    protected void callTosDeckingUpdate(
            MessageMeta msgMeta,
            AiconDeckingEngineResponse aiconDeckingResponse,
            List<Condition> tosConditions
    ) {
        // Prepare and send decking update request to TOS
        AiconYardDeckingUpdateRequestMessage tosRequest = AiconYardDeckingUpdateRequestMessageHelper.createWithDefaults();
        tosRequest.setRequestId(aiconDeckingResponse.getRequestIndex());
        tosRequest.setTimeStamp(System.currentTimeMillis());
        tosRequest.setTimeStampLocalTimeISO8601(Instant.ofEpochMilli(tosRequest.getTimeStamp()));

        // Convert response tosRequest(s) to move(s)
        List<DeckMove> deckMoves = new ArrayList<>();
        int idx = 0;
        for (AiconDeckingEngineResponse.Request responseReq : aiconDeckingResponse.getRequests()) {
            DeckMove move = new DeckMove();
            move.setPositionTo(
                    PositionConverter.mergePosition(
                            PositionConverter.convertPosPartsToTos(
                                    PositionConverter.collectParts(
                                            responseReq.getBlockIndexNumber(),
                                            responseReq.getBayIndexNumber(),
                                            responseReq.getRowIndexNumber(),
                                            responseReq.getTierIndexNumber()
                                    )
                            )
                            , false, false
                    )
            );
            // need to read the gkey from the response, no guarantee they arrive in the same order as the tosRequest
            move.setWiGkey(responseReq.getInternalIndex1());
            deckMoves.add(move);
            msgMeta.setEntityValues(idx, msgMeta.getEntityValues(idx) + "→{}", move.getPositionTo());
            idx++;
        }

        if (deckMoves.isEmpty()) {
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                    "No deckMove entries created for the request to {}, scenario ends!",
                    tosDeckingRequestProducer.getTopicName());
            return;
        } else {
            tosRequest.setMoves(deckMoves);
            tosRequest.setValidations(tosConditions);
        }

        // Send the actual request to TOS and wait for the response
        sendRecvTosDeckingUpdate(msgMeta, tosRequest);
    }


    private GenericRecord sendRecvTosDeckingUpdate(
            MessageMeta msgMeta,
            AiconYardDeckingUpdateRequestMessage request
    ) {
        String requestTopic = tosDeckingRequestProducer.getTopicName();
        String responseTopic = tosDeckingResponseManager.getTopicName();

        String uniqueKey = request.getRequestId().toString();

        CompletableFuture<GenericRecord> tosDeckingFuture = tosDeckingResponseManager.registerAndGetFuture(uniqueKey)
                .orTimeout(tosResponseTimeoutS, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                            "Timeout({}s)/error for {} with {}, reason: {}",
                            tosResponseTimeoutS, responseTopic, uniqueKey, ex);
                    return null;
                });

        try {
            // SEND TO TOPIC
            msgMeta.addTimestampWithPrefix(MessageMeta.TS_SEND_PREFIX, requestTopic, getLogger());
            tosDeckingRequestProducer.sendTosDeckingUpdateMessage(request);

            // WAIT FOR RESPONSE
            GenericRecord matchedResponse = tosDeckingFuture.get();

            if (matchedResponse != null) {
                TosDeckingUpdateResponse tosResponse = TosDeckingUpdateResponse.fromGenericRecord(matchedResponse);
                msgMeta.setResultWhenHigher(getLogger(), tosResponse.getWorseResult());
                msgMeta.addTimestampWithPrefix(MessageMeta.TS_RECV_PREFIX, responseTopic, getLogger());
                getLogger().info(AnsiColor.blue("Decking response from {} matched with key {}: {}."), responseTopic, uniqueKey, tosResponse.getWorseResult());
            } else {
                msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                        "Timeout({}s)/error while waiting for a matching {} with key: {}.",
                        tosResponseTimeoutS, responseTopic, uniqueKey);
            }
            return matchedResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                    "Interrupted while waiting for response from {} with key {}, reason: {}",
                    responseTopic, uniqueKey, e.getMessage());
            return null;
        } catch (KafkaException e) {
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR, e.getMessage());
            return null;
        } catch (Exception e) {
            msgMeta.setResultWhenHigher(getLogger(), ResultLevel.ERROR,
                    "Unexpected error while waiting for response from {} with key {}, reason: {}",
                    responseTopic, uniqueKey, e.getMessage());
            return null;
        }
    }

    /**
     * @return a generated unique key with {@link #UNIQUE_ID_PFX} prefix for this request
     */
    protected String createUniqueId() {
        String uniqueKey = (UNIQUE_ID_PFX + UUID.randomUUID()).substring(0, 25);
        getLogger().debug("Generated unique request key: {}", uniqueKey);
        return uniqueKey;
    }

    public String toString() {
        String text = super.toString();
        if (aiconDeckingResponseManager != null) {
            text += String.format("\n    AiconDecking-Consumer: %s, Producer: %s", aiconDeckingResponseManager.getConsumer().getStatus(), aiconDeckingRequestProducer.getStatus());
        }
        if (tosDeckingResponseManager != null) {
            text += String.format("\n    TosDecking-Consumer  : %s, Producer: %s", tosDeckingResponseManager.getConsumer().getStatus(), tosDeckingRequestProducer.getStatus());
        }
        return text;
    }
}
