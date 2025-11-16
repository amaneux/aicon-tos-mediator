package com.aicon.tos.interceptor.decide.scenarios;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.InterceptorEntityConfig;
import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.avlino.common.MetaField;

import java.util.List;

/**
 * Base class for any scenario with some common functionality.
 */
abstract public class ScenarioBase implements Scenario {
    protected volatile boolean running = false;     // set to true when connectors are online
    protected long countOk = 0;
    protected long countError = 0;
    protected String scenarioName = this.getClass().getSimpleName();
    protected String entityName = null;
    protected InterceptorEntityConfig config = null;
    protected List<MetaField<?>> filterChanges = null;
    protected List<CDCAction> filterActions = null;

    /**
     * Sets filter definitions for {@link #filterChanges} and {@link #filterActions}.
     * When only fields are set, actions defaults to {@link CDCAction#CHANGED}.
     */
    abstract protected void setFilters();

    @Override
    public boolean addsMessageMeta() {
        return true;
    }

    @Override
    public boolean isRelevantEvent(CollectedMessage event) {
        if (event == null) {
            return false;
        }

        boolean relevant = getFilterActions() != null && getFilterActions().contains(event.getCDCAction());

        if (relevant && CDCAction.CHANGED == event.getCDCAction()) {
            // check if there are any fields to filter on and if so they should match at least 1 of them
            if (filterChanges != null) {
                relevant = false;
                for (MetaField<?> field : filterChanges) {
                    if (event.hasChanged(field.id())) {
                        relevant = true;
                        break;
                    }
                }
            }
        }
        return relevant;
    }

    public List<CDCAction> getFilterActions() {
        return filterActions != null ? filterActions : List.of(CDCAction.CHANGED);
    }

    protected String getUptUserId() {
        return DEFAULT_USER_ID;
    }

    protected String getProgramId() {
        return this.getClass().getSimpleName();
    }

    public String getName() {
        return scenarioName;
    }

    public String getEntityName() {
        return entityName;
    }

    public void init(String name, InterceptorEntityConfig config) {
        this.scenarioName = name;
        this.entityName = config.getEntityName();
        this.config = config;
        setFilters();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public void setCounters(ResultEntry result) {
        if (result == null) {
            return;
        }
        if (ResultLevel.ERROR == result.getLevel()) {
            countError++;
        } else {
            countOk++;
        }
    }

    @Override
    public void stop() {
        running = false;
        getLogger().info("[{}] Stopping scenario...", scenarioName);
    }

    public String toString() {
        return String.format("  Scenario %s: entity=%s, actions=%s, ok/error=%s/%s, running=%s",
                scenarioName, entityName, getFilterActions(), countOk, countError, isRunning());
    }
}
