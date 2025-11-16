package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.aicon.tos.shared.config.ConfigUtil.getConfigItem;

public abstract class AbstractSessionController {

    private final ConfigSettings configSettings;
    protected ConfigGroup kafkaConfig;
    private ConfigGroup flowConfigs;
    protected ConfigGroup httpConfig;
    private ConfigGroup generalConfig;
    private ConfigGroup connectionsConfig;
    private ConfigGroup canaryCheckConfig;

    private final DataStore dataStore = DataStore.getInstance();
    private final FlowManager flowManager;
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionController.class);

    private final List<FlowSession> activeSessions = new ArrayList<>();
    private String n4Scope;
    private int requestId = 0;
    private ConfigGroup tosControlConfig;


    protected AbstractSessionController(FlowManager flowManager) {
        this.flowManager = flowManager;
        this.configSettings = this.flowManager.getConfigSettings();
        getConfiguration();
    }

    private void getConfiguration() {

        flowConfigs = configSettings.getMainGroup(ConfigType.Flows);
        connectionsConfig = configSettings.getMainGroup(ConfigType.Connections);

        tosControlConfig = configSettings.getMainGroup(ConfigType.TosControl);
        generalConfig = configSettings.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
        canaryCheckConfig = tosControlConfig.getChildGroup(ConfigType.CanaryCheck);

        n4Scope = getConfigItem(generalConfig, ConfigDomain.CFG_YARD_SCOPE, String.class, "?");
    }

    public void sessionEnded(FlowSession session) {
        activeSessions.remove(session);
    }

    public void setSessionState(FlowSession.SessionState state, String message) {
        // default: log or no-op
    }

    public List<FlowSession> getActiveSessions() {
        return Collections.unmodifiableList(activeSessions);
    }

    public String getNewControlRequestId() {
        String requestIdStr = String.format("%s-%d", flowConfigs.getName(), ++requestId); //TODO Check...
        LOG.info("New requestId: {}", requestIdStr);
        return requestIdStr;
    }

    public ConfigGroup getKafkaConfig() {
        return kafkaConfig;
    }

    public ConfigGroup getHttpConfig() {
        return httpConfig;
    }

    public String getN4Scope() {
        return n4Scope;
    }

    public int getRequestId() {
        return requestId;
    }

    public FlowManager getFlowManager() {
        return flowManager;
    }

    public ConfigGroup getConnectionsConfig() {
        return connectionsConfig;
    }

    public ConfigGroup getCanaryCheckConfig() {
        return canaryCheckConfig;
    }

    public ConfigGroup getGeneralConfig() {
        return generalConfig;
    }

    public ConfigSettings getConfigSettings() {
        return configSettings;
    }

    public DataStore getDataStore() {
        return dataStore;
    }
}

