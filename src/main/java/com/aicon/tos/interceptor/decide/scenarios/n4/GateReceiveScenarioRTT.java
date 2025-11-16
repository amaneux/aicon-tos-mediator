package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.decide.StoredMessageSearcher;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.N4EventBase;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.RoadTruckTransactionEvent;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Gate Receive scenario for Road Truck Transaction
 * This scenario processes inv_wi messages and road_truck_transactions messages when a related combo is received
 * a decking update request is made to TOS
 */
public class GateReceiveScenarioRTT extends GateReceiveScenario {
    private static final Logger LOG = LoggerFactory.getLogger(GateReceiveScenarioRTT.class);

    @Override
    protected void setFilters() {
        filterActions = List.of(CDCAction.CREATED);
    }

    @Override
    public void processMessage(FilteredMessage newMessage, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {
        if (!isRunning()) {
            LOG.warn("{} Scenario is not active (yet). Ignoring message.", scenarioName);
            return;
        }

        RoadTruckTransactionEvent tranEvent = N4EventBase.createInstance(newMessage);

        LOG.info("Processing new message from entity {}: {}", tranEvent.getEntityName(), tranEvent);
        handleRoadTruckTransactions(tranEvent, globalMessageStorage);
    }

    private void handleRoadTruckTransactions(RoadTruckTransactionEvent tranEvent, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {

        StoredMessageSearcher searcher = new StoredMessageSearcher(globalMessageStorage);

        Predicate<FilteredMessage> gkeyPredicate = message -> message.getFields().stream()
                .anyMatch(field -> isMatchingField(field, WorkInstructionEvent.FLD_TRAN_GKEY, tranEvent.getGkey()));
        List<FilteredMessage> results = searcher.findMessagesByCondition(ENTITY_WI, gkeyPredicate);

        if (!results.isEmpty()) {
            LOG.info("Found {} matching {} with reference key: {}.", results.size(), ENTITY_WI, tranEvent.getGkey());
            results.forEach(message -> LOG.info("Processing message: {}", message.getPayloadAsJson()));
            sendTosDeckingUpdateMessage(N4EventBase.createInstance(results.get(0)), tranEvent);
            setCounters(tranEvent.getMsg().meta().getResult());
        } else {
            LOG.warn("No matching inv_wi found with reference: {}.", tranEvent.getGkey());
        }
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}