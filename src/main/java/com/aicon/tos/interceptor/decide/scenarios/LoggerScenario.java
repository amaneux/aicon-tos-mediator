package com.aicon.tos.interceptor.decide.scenarios;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.InterceptorEntityConfig;
import com.aicon.tos.shared.ResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;

/**
 * Logs all the messages as defined by {@link #isRelevantEvent(CollectedMessage)}.
 */
public class LoggerScenario extends ScenarioBase {
    private static final Logger LOG = LoggerFactory.getLogger(LoggerScenario.class);

    @Override
    protected void setFilters() {
        filterActions = List.of(CDCAction.CREATED, CDCAction.CHANGED, CDCAction.DELETED);
        filterChanges = null;   // when null, we get any message which has CHANGED (no need to set here, but just mentioned here for the sake of it).
    }

    @Override
    public boolean addsMessageMeta() {
        return true;    // todo set to false after testing
    }

    @Override
    public void init(String name, InterceptorEntityConfig config) {
        super.init(name, config);
        running = true;
    }

    @Override
    public boolean isRelevantEvent(CollectedMessage event) {
        if (!super.isRelevantEvent(event)) {        // basic validations
            return false;
        }

        // here your specialized filter requirements besides the basic checks on defined filterActions and filterFields

        // Note: Any message passing this call as true will be stored in the globalMessageStorage and can be referenced later on via #processMessage.

        return true;
    }

    @Override
    public void processMessage(FilteredMessage newMessage, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {
        if (!isRunning()) {
            LOG.warn("{} Scenario is not active (yet). Ignoring message.", scenarioName);
            return;
        }

        LOG.info("Received {}", newMessage.toStringExtended());
    }

    @Override
    public Logger getLogger() {
        return LOG;
    }
}