package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.tos.interceptor.InterceptorEntityConfig;
import com.aicon.tos.interceptor.decide.ResponseManager;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.RoadTruckTransactionEvent;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingRequestProducer;
import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingResponseConsumer;
import com.aicon.tos.interceptor.newgenproducerconsumer.KafkaGenericConsumerBase;
import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestProducer;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateResponseConsumer;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import com.aicon.tos.shared.schema.DeckMove;
import com.aicon.tos.shared.util.AnsiColor;
import org.apache.avro.generic.GenericRecord;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * Base class for Gate Receive scenarios.
 */
public abstract class GateReceiveScenario extends DeckingScenarioBase {
    protected static final String ENTITY_RTT = RoadTruckTransactionEvent.ENTITY_NAME;
    protected static final String ENTITY_WI = WorkInstructionEvent.ENTITY_NAME;


    public void init(String name, InterceptorEntityConfig entityName) {
        super.init(name, entityName);
        tosDeckingRequestProducer = new AiconYardDeckingUpdateRequestProducer();
        initConnectors();
    }

    /**
     * Initializes the consumers when not initialized yet.
     * @return true when all consumers are ready, else recall later to try again.
     */
    protected boolean initConnectors() {
        if (isRunning()) {
            return true;
        }
        try {
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
            getLogger().info("Connector successfully started");
            return true;
        } else {
            getLogger().error("Failed connecting. Check VPN, kafka bootstrap servers");
            return false;
        }
    }

    @Override
    public boolean isRunning() {
        boolean run1 = tosDeckingResponseManager != null && tosDeckingResponseManager.getConsumer().getStatus().getState().isRunning();
        return super.isRunning() && run1;
    }


    protected GenericRecord sendTosDeckingUpdateMessage(WorkInstructionEvent invWi, RoadTruckTransactionEvent tran) {
        if (!initConnectors()) {     // check if all consumers are ready, else no use to process this message (gets lost).
            return null;
        }

        getLogger().info("Preparing decking-update-request for wiGkey={}, tranGkey={}", invWi.getMessageKey(), tran.getGkey());

        AiconYardDeckingUpdateRequestMessage request = new AiconYardDeckingUpdateRequestMessage();
        request.setRequestId(tosDeckingRequestProducer.getNewId());
        request.setTimeStamp(Instant.now().toEpochMilli());
        request.setTimeStampLocalTimeISO8601(request.getTimeStampLocalTimeISO8601());

        DeckMove move = new DeckMove();
        move.setPositionTo(tran.getCtrPosName());
        move.setWiGkey(String.valueOf(invWi.getGkey()));

        request.setMoves(List.of(move));

        if (getLogger().isInfoEnabled()) {
            move = request.getMoves().get(0);
            getLogger().info(AnsiColor.brightMagenta("Sending TOS decking update request with id={}, positionTo={}, wiGKey={}"),
                    request.getRequestId(), move.getPositionTo(), move.getWiGkey());
        }
        String uniqueKey = request.getRequestId().toString();

        CompletableFuture<GenericRecord> deckingFuture = tosDeckingResponseManager.registerAndGetFuture(uniqueKey)
                .orTimeout(60, TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    getLogger().error(AnsiColor.red("Timeout or error for " + uniqueKey + ": " + ex.getMessage()));
                    return null;
                });

        tosDeckingRequestProducer.sendTosDeckingUpdateMessage(request);

        try {
            GenericRecord matchedResponse = deckingFuture.get();
            if (matchedResponse != null) {
                getLogger().info(AnsiColor.brightBlue("Decking response matched for {}: {}."), uniqueKey, matchedResponse);
            } else {
                getLogger().warn(AnsiColor.red("Timeout or error while waiting for a matching response for key: {}."),
                        uniqueKey);
            }
            return matchedResponse;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            getLogger().error("Interrupted while waiting for response for key {}: {}",
                    uniqueKey, e.getMessage(), e);
            return null;
        } catch (Exception e) {
            getLogger().error("Unexpected error while waiting for response for key {}: {}",
                    uniqueKey, e.getMessage(), e);
            return null;
        }
    }
}