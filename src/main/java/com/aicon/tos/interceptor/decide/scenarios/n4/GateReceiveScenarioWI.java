package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.decide.StoredMessageSearcher;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.N4EventBase;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.RoadTruckTransactionEvent;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WiMoveKindEnum;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

import static com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent.*;

/**
 * Gate Receive scenario for WorkInstructions.
 * This scenario processes inv_wi messages and road_truck_transactions messages when a related combo is received
 * a decking update request is made to TOS
 */
public class GateReceiveScenarioWI extends GateReceiveScenario {

    private static final Logger LOG = LoggerFactory.getLogger(GateReceiveScenarioWI.class);
    public static final String ESF_IMMINENT_MOVE = "IMMINENT_MOVE";

    @Override
    protected void setFilters() {
        filterChanges = List.of(MF_EC_STATE_FETCH);
    }

    @Override
    public boolean isRelevantEvent(CollectedMessage event) {
        if (!super.isRelevantEvent(event)) {    // basic validations on action and changed fields
            return false;
        }

        String moveKind = event.getFieldValueAsString(FLD_MOVE_KIND);
        if (!WiMoveKindEnum.RECEIVAL.getTosCode().equals(moveKind)) {
            if (LOG.isTraceEnabled()) LOG.trace("move_kind {} is not gate receive", moveKind);
            return false;
        }

        String ecStateFetch = event.getFieldValueAsString(FLD_EC_STATE_FETCH);
        if (!ESF_IMMINENT_MOVE.equals(ecStateFetch)) {
            if (LOG.isTraceEnabled()) LOG.trace("ec_state_fetch {} is not {}", ecStateFetch, ESF_IMMINENT_MOVE);
            return false;
        }
        return true;
    }

    @Override
    public void processMessage(FilteredMessage newMessage, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {

        if (!isRunning()) {
            LOG.warn("{} Scenario is not active (yet). Ignoring message.", scenarioName);
            return;
        }

        LOG.info("Processing new message from entity {}: {}", newMessage.getEntityName(), newMessage);
        handleInvWi(N4EventBase.createInstance(newMessage), globalMessageStorage);
    }

    private void handleInvWi(WorkInstructionEvent wiEvent, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {

        LinkedList<FilteredMessage> storedMessages = globalMessageStorage.getOrDefault(wiEvent.getEntityName(), new LinkedList<>());

        LOG.info("Found {} stored messages for entity '{}'.", storedMessages.size(), wiEvent.getEntityName());

        Long tranGkey = wiEvent.getTranGkey();
        LOG.info("Handle receive logic for gkey {}. Truck Visit Ref: {}", wiEvent.getGkey(), tranGkey);

        if (tranGkey != null) {
            Predicate<FilteredMessage> tranPredicate = message ->
                    message.getFields().stream()
                            .anyMatch(field -> isMatchingField(field, RoadTruckTransactionEvent.FLD_GKEY, tranGkey));

            StoredMessageSearcher searcher = new StoredMessageSearcher(globalMessageStorage);
            List<FilteredMessage> results = searcher.findMessagesByCondition(ENTITY_RTT, tranPredicate);

            if (!results.isEmpty()) {
                sendTosDeckingUpdateMessage(wiEvent, N4EventBase.createInstance(results.get(0)));
                setCounters(wiEvent.getMsg().meta().getResult());
            } else {
                LOG.warn("No matching TruckTransaction found for tranGkey: {}.", tranGkey);
            }
        }
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}