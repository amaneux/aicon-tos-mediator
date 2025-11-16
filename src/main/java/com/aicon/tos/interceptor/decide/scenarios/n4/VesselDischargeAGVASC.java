package com.aicon.tos.interceptor.decide.scenarios.n4;

import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.InterceptorValueObject;
import com.aicon.tos.interceptor.MessageMeta;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.N4EventBase;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WiMoveKindEnum;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import com.aicon.tos.shared.schema.Condition;
import com.aicon.tos.shared.util.AnsiColor;
import com.avlino.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

import static com.aicon.tos.interceptor.MessageMeta.TS_END_PREFIX;
import static com.aicon.tos.interceptor.MessageMeta.TS_START_PREFIX;
import static com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent.*;

/**
 * Vessel Discharge scenario for AGV-ASC operations with a split WI, 1 for the AGV and 1 for the ASC. The original planned
 * WI gets split into 2, the original with move_number 1.0 brings the container from the QC-area to the ASC transfer lane
 * and the newly created with move_number 2.0, to bring the container into a stack of the ASC block. There is also a
 * position change involved, the original WI starts with a detailed ASC slot, and once move_number 2.0 is created, it
 * hands over the slot position to move_number 2.0 and move_number 1.0 gets the transfer lane as destination.
 *
 * This scenario processes inv_wi messages for move_kind=DSCH, and
 *     move_number 1.0 and move_stage from PLANNED -> FETCH_UNDERWAY   (determines basically the block)
 *     move_number 2.0 and move_stage from PLANNED -> CARRY_READY      (re-deck when ASC has started)
 */

public class VesselDischargeAGVASC extends DeckingScenarioBase {
    private static final Logger LOG = LoggerFactory.getLogger(VesselDischargeAGVASC.class);

    @Override
    protected void setFilters() {
        filterChanges = List.of(MF_MOVE_STAGE);
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

            Double moveNumber = event.getFieldValueAsDouble(FLD_MOVE_NUMBER, 0.0D);
            if (!(moveNumber == 1.0D || moveNumber == 2.0D)) {
                if (LOG.isTraceEnabled()) LOG.trace("{} is not 1 or 2.", FLD_MOVE_NUMBER);
                return false;
            }

            InterceptorValueObject moveStageVO = event.getFieldValue(FLD_MOVE_STAGE);
            if (!MoveStage.PLANNED.name().equals(moveStageVO.beforeValueAsString(null))) {
                if (LOG.isTraceEnabled()) LOG.trace("{} was not PLANNED before", FLD_MOVE_STAGE);
                return false;
            }

            if (!(moveNumber == 1.0D && MoveStage.FETCH_UNDERWAY.name().equals(moveStageVO.afterValueAsString(null))) &&
                !(moveNumber == 2.0D && MoveStage.CARRY_READY   .name().equals(moveStageVO.afterValueAsString(null)))
            ) {
                if (LOG.isTraceEnabled()) LOG.trace("{} expected to change to FETCH_UNDERWAY|CARRY_READY", FLD_MOVE_STAGE);
                return false;
            }

            return true;
        } catch (Exception exc) {       // make this method failsafe
            LOG.error("Error while validating event {}, reason: {}", event.toStringExtended(), exc.getMessage());
            return false;
        }
    }

    @Override
    public void processMessage(FilteredMessage event, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {
        if (!isRunning()) {
            LOG.warn("Scenario is not active (yet). Ignoring message.");
            return;
        }

        event.meta().addTimestampWithPrefix(TS_START_PREFIX, scenarioName, LOG);
        WorkInstructionEvent wiEvent = N4EventBase.createInstance(event);

        Double moveNumber = event.getFieldValueAsDouble(FLD_MOVE_NUMBER, 0.0D);
        LOG.info("Handling vessel discharge logic for offset={}, {}, move_number={}",
                wiEvent.getMsg().getOffset(), wiEvent.getMoveStage(), moveNumber);

        WorkInstructionEvent otherWi = null;
        boolean requestedBlockOnly;
        List<Condition> tosConditions = new ArrayList<>(2);

        if (moveNumber == 1.0D) {       // basically we want to select a block here
            // And allow TOS only to update when move_stage is still the same, otherwise we might update WI's already progressed to far.
            tosConditions.add(new Condition(FLD_MOVE_STAGE, String.join(Constants.COMMA, new String[] {MoveStage.FETCH_UNDERWAY.name()})));
            requestedBlockOnly = false;
            TwinWith tw = wiEvent.getTwinWith(TwinWith.NONE);

            // In Automated terminals we can trust the twin flags, because the system won't deviate from the plan
            if (tw != TwinWith.NONE) {      // so this move is part of a twin, see if we can find the other one.
                List<FilteredMessage> list = globalMessageStorage.getOrDefault(wiEvent.getEntityName(), new LinkedList<>());
                // walk through the list in reverse order and find the first other WI as part of this twin
                long otherSeq;
                TwinWith otherTW;
                long wqGkey = wiEvent.getWqGkey();
                if (tw == TwinWith.NEXT) {
                    otherSeq = wiEvent.getSequence() + 1;
                    otherTW = TwinWith.PREV;
                } else {
                    otherSeq = wiEvent.getSequence() - 1;
                    otherTW = TwinWith.NEXT;
                }

                for (int i = list.size() - 1; i >= 0; i--) {
                    FilteredMessage other = list.get(i);
                    if (other.getFieldValueAsLong(FLD_WQ_GKEY, -1L) == wqGkey &&
                            other.getFieldValueAsLong(FLD_SEQUENCE, -1L) == otherSeq &&
                            wiEvent.getTwinWith(other.getFieldValueAsString(FLD_TWIN_WITH), TwinWith.NONE) == otherTW
                    ) {
                        // so we found the other Twin WorkInstruction
                        otherWi = N4EventBase.createInstance(other);
                        break;
                    }
                }
                if (otherWi == null) {
                    // we can skip this execution, because the other twin WI is not there yet, and this will be repeated
                    // once the other twin WI appears (and that should find this WI as its companion).
                    LOG.info("WI-companion not found yet, so skipping further processing for {} ", wiEvent);
                    return;
                }
            }
        } else if (moveNumber == 2.0D) {        // now we're at the transfer-zone, re-deck in case something has changed (re-decked by Navis, occupied, etc).
            tosConditions.add(new Condition(FLD_MOVE_STAGE,
                    String.join(Constants.COMMA, new String[] {MoveStage.CARRY_READY.name()})));
            tosConditions.add(new Condition(FLD_DEFINITE, Constants.DASH));     // means, do not verify on definite flag
            requestedBlockOnly = true;
            LOG.info("Re-decking at transfer zone for wi-gkey {}", wiEvent.getGkey());
        } else {                                // we should not get here, because it was checked in isRelevantEvent.
            return;
        }

        List<WorkInstructionEvent> wiList;
        if (otherWi == null) {
            LOG.info(AnsiColor.brightGreen("Sending single lift message for wi-gkey {}"), wiEvent.getGkey());
            wiList = List.of(wiEvent);
        } else {
            LOG.info(AnsiColor.brightGreen("Sending twin lift message for wi-gkeys {}, {}"), wiEvent.getGkey(), otherWi.getGkey());
            wiList = List.of(wiEvent, otherWi);
        }
        MessageMeta meta = event.meta();
        sendDeckingMessageToAiconAndTOS(meta, wiList, requestedBlockOnly, tosConditions);

        setCounters(meta.getResult());
        meta.addTimestampWithPrefix(TS_END_PREFIX, scenarioName, LOG);
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}