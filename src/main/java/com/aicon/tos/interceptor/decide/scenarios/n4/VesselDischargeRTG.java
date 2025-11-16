package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.N4EventBase;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WiMoveKindEnum;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.util.AnsiColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static com.aicon.tos.interceptor.MessageMeta.*;
import static com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent.*;

/**
 * Vessel Discharge scenario for RTG operations.
 * This scenario processes inv_wi messages with move_kind=DSCH and itv_gkey changed
 */

public class VesselDischargeRTG extends DeckingScenarioBase {
    private static final Logger LOG = LoggerFactory.getLogger(VesselDischargeRTG.class);
    private final long MAX_ITV_CYCLE_TIME_MS = 30 * 60 * 1000;

    @Override
    protected void setFilters() {
        filterChanges = List.of(MF_POS_NAME, MF_ITV_GKEY);
    }

    @Override
    public boolean isRelevantEvent(CollectedMessage event) {
        if (!super.isRelevantEvent(event)) {    // basic validations on action and changed fields
            return false;
        }

        try {
            String moveKind = event.getFieldValueAsString(FLD_MOVE_KIND);
            if (!WiMoveKindEnum.VESL_DISCH.getTosCode().equals(moveKind)) {
                if (LOG.isTraceEnabled()) LOG.trace("move_kind {} is not vessel discharge", moveKind);
                return false;
            }

            if (!event.hasChanged(FLD_ITV_GKEY) || event.getFieldValueAsLong(FLD_ITV_GKEY, null) == null) {
                if (LOG.isTraceEnabled()) LOG.trace("{} has not changed or changed to null.", FLD_ITV_GKEY);
                return false;
            }
            return true;
        } catch (Exception exc) {       // make this method failsafe
            LOG.error("Error while validating event {}, reason: {}", event.toStringExtended(), exc.getMessage());
            return false;
        }
    }

    @Override
    public void processMessage(FilteredMessage newMessage, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {
        if (!isRunning()) {
            LOG.warn("Scenario is not active (yet). Ignoring message.");
            return;
        }

        LOG.info("Processing new message from entity {}: {}", newMessage.getEntityName(), newMessage);
        newMessage.meta().addTimestampWithPrefix(TS_START_PREFIX, scenarioName, LOG);
        WorkInstructionEvent wiEvent = N4EventBase.createInstance(newMessage);

        LinkedList<FilteredMessage> storedMessages = globalMessageStorage.getOrDefault(wiEvent.getEntityName(), new LinkedList<>());

        LOG.info("Found {} stored messages for entity '{}'.", storedMessages.size(), wiEvent.getEntityName());

        Long itvGkey = wiEvent.getItvGkey();

        LOG.info(AnsiColor.brightYellow("Handling vessel discharge logic for gkey {}. itvGkey: {}"),
                wiEvent.getGkey(), itvGkey);

        if (itvGkey != null) {
            WorkInstructionEvent otherWi = null;
            List<FilteredMessage> list = globalMessageStorage.getOrDefault(wiEvent.getEntityName(), new LinkedList<>());
            // walk through the list in reverse order and find the first other WI within given timeframe
            for (int i = list.size() - 1; i >= 0; i--) {
                FilteredMessage other = list.get(i);
                if (wiEvent.getMsg().compareFieldWithOther(FLD_ITV_GKEY, other)
                        && !wiEvent.getMsg().compareFieldWithOther(WorkInstructionEvent.FLD_GKEY, other)) {
                    // so we found the other WorkInstruction for the same ITV in the time window
                    otherWi = N4EventBase.createInstance(other);
                    break;
                }
                if (wiEvent.getCdCReceivedTimestamp().toEpochMilli() - other.meta().getTimestamp(TS_CDC_RECEIVED).toEpochMilli() > MAX_ITV_CYCLE_TIME_MS) {
                    break;
                }
            }

            List<WorkInstructionEvent> wiList = null;
            if (otherWi == null) {
                LOG.info("[{}] No matching messages found for itv_gkey: {}, sending single lift message ", scenarioName, itvGkey);
                wiList = List.of(wiEvent);
            } else {
                LOG.info("[{}}] matching message found for itv_gkey: {}, sending twin lift message ", scenarioName, itvGkey);
                wiList = List.of(wiEvent, otherWi);
            }

            if (wiList != null) {       // we have something to send, so prepare the request and send to aicon & tos
                sendDeckingMessageToAiconAndTOS(wiEvent.getMsg().meta(), wiList, true, null);
            }

        }
        setCounters(newMessage.meta().getResult());
        newMessage.meta().addTimestampWithPrefix(TS_END_PREFIX, scenarioName, LOG);
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}