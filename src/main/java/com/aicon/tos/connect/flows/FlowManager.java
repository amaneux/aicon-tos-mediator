package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The singleton FlowManager issues n FlowControllers based on the flow definitions found in the configuration.
 * Each FlowController will be started in its own Thread.
 */
public class FlowManager {
    private static final Logger LOG = LoggerFactory.getLogger(FlowManager.class.getName());

    private static FlowManager instance = null;

    private final ConfigSettings configSettings;
    public Map<FlowController, Thread> controllers;
    public Map<CanaryController, Thread> canaryControllers;
    public Map<CDCController, Thread> cdcControllers;
    private String n4Scope;

    public static FlowManager getInstance() {
        synchronized (LOG) {
            if (instance == null) {
                instance = new FlowManager();
            }
            return instance;
        }
    }

    // Support for unit testing
    public static FlowManager getInstance(ConfigSettings configSettings) {
        synchronized (LOG) {
            if (instance == null) {
                instance = new FlowManager(configSettings);
            }
            return instance;
        }
    }

    private FlowManager() {
        this(ConfigSettings.getInstance());
    }

    private FlowManager(ConfigSettings configSettings) {
        LOG.info("Instantiate Flowmanager");
        this.configSettings = configSettings;
        controllers = new LinkedHashMap<>();
        canaryControllers = new LinkedHashMap<>();
        cdcControllers = new LinkedHashMap<>();
    }

    public static FlowManager forTestConfig(String configFilePath) {
        ConfigSettings.setConfigFile(configFilePath);
        return new FlowManager(ConfigSettings.getInstance());
    }

    public void init() {
        if (!configSettings.hasStorageError()) {
            ConfigGroup flowConfigs = configSettings.getMainGroup(ConfigType.Flows);

            ConfigGroup tosControlConfig = configSettings.getMainGroup(ConfigType.TosControl);
            ConfigGroup generalConfig = configSettings.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
            ConfigGroup canaryConfig = tosControlConfig.getChildGroup(ConfigType.CanaryCheck);
            ConfigItem scopeConfig = generalConfig.findItem(ConfigDomain.CFG_YARD_SCOPE);

            if (scopeConfig != null) {
                n4Scope = scopeConfig.value();
            } else {
                LOG.error("YardScope undefined!");
            }

            if (canaryConfig != null && (canaryConfig.getItemValue(ConfigDomain.CFG_CANARY_ON_OFF) == null ||
                    !canaryConfig.getItemValue(ConfigDomain.CFG_CANARY_ON_OFF).equalsIgnoreCase("off"))) {
                CanaryController canaryController = new CanaryController(this);
                Thread thread = new Thread(canaryController, canaryController.getName());
                canaryControllers.put(canaryController, thread);
            }

            CDCController cdcController = new CDCController(this);
            Thread thread = new Thread(cdcController, cdcController.getName());
            cdcControllers.put(cdcController, thread);

            for (ConfigGroup flow : flowConfigs.getChildren()) {
                if (ConfigType.Flow == flow.getType()) {
                    FlowController controller = new FlowController(this, flow);
                    thread = new Thread(controller, flow.getName());
                    controllers.put(controller, thread);
                }
            }
            LOG.info("FlowManager initiated");
        } else {
            LOG.error("FlowManager not initiated, configuration error: {}", configSettings.getStorageError());
        }
    }

    public void start() {
        if (configSettings.hasStorageError()) {
            LOG.error("FlowManager not started, configuration error: {}", configSettings.getStorageError());
        } else {
            LOG.info("Starting FlowManager...");

            startControllerThreads(controllers, "FlowController");
            startControllerThreads(canaryControllers, "CanaryController");
            startControllerThreads(cdcControllers, "CDCController");

            LOG.info("FlowManager started!");
        }
    }

    private <T> void startControllerThreads(Map<T, Thread> controllerMap, String name) {
        if (controllerMap == null) {
            throw new IllegalStateException(name + " not initialized");
        }

        for (Map.Entry<T, Thread> entry : controllerMap.entrySet()) {
            Thread thread = entry.getValue();
            if (thread != null && !thread.isAlive()) {
                thread.start();
            }
        }
    }

    public void stop() {
        LOG.info("Stopping FlowManager, stopping {} controllers, " +
                "{} canarycontrollers and {} cdccontrollers", controllers.size(), canaryControllers.size(), cdcControllers.size());
        stopAndRemoveAllInactiveControllers(
                List.of(controllers,
                        canaryControllers,
                        cdcControllers
                )
        );

        LOG.info("FlowManager stopped!");
    }

    private void stopAndRemoveAllInactiveControllers(List<Map<? extends Runnable, Thread>> controllerMaps) {
        for (Map<? extends Runnable, Thread> controllersToBeStopped : controllerMaps) {
            stopAndRemoveInactiveControllers(controllersToBeStopped);
        }
    }

    private void stopAndRemoveInactiveControllers(Map<? extends Runnable, Thread> controllers) {
        Iterator<? extends Map.Entry<? extends Runnable, Thread>> iterator = controllers.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<? extends Runnable, Thread> entry = iterator.next();
            Runnable controller = entry.getKey();
            Thread thread = entry.getValue();

            // Call stopController() if the controller is a stoppable
            if (controller instanceof StoppableController) {
                ((StoppableController) controller).stopController();
            }

            // Ensure the thread stops
            if (thread != null) {
                try {
                    thread.join(1000); // Wait for 1 second for the thread to finish
                    if (thread.isAlive()) {
                        // If still alive, interrupt the thread again to force stop
                        thread.interrupt();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // Reset interrupted flag for safety
                    LOG.warn("Interrupted while waiting to stop thread for controller: {}", controller);
                }
            }

            // Remove the entry if the thread is no longer alive
            if (thread != null && !thread.isAlive()) {
                iterator.remove();
            }
        }
    }

    public String toString() {
        if (!controllers.isEmpty()) {
            StringBuilder clog = new StringBuilder("Connectors:");
            for (ConfigGroup conn : configSettings.getMainGroup(ConfigType.Connections).getChildren()) {
                switch (conn.getType()) {
                    case Kafka:
                        clog.append("\n Kafka connector:");
                        clog.append(String.format(" host:port = %s:%s",
                                conn.getItemValue(ConfigSettings.CFG_KAFKA_HOST),
                                conn.getItemValue(ConfigSettings.CFG_KAFKA_PORT)));
                        clog.append(String.format(" %s = %s", ConfigSettings.CFG_KAFKA_GROUP_ID,
                                conn.getItemValue(ConfigSettings.CFG_KAFKA_GROUP_ID)));
                        break;
                    case Http:
                        clog.append(String.format("\n HTTP connector (%s):", conn.getName()));
                        clog.append(String.format(" %s = %s", ConfigSettings.CFG_HTTP_URL,
                                conn.getItemValue(ConfigSettings.CFG_HTTP_URL)));
                        break;
                    default:
                }
            }
            clog.append("\n\nFlowControllers:");
            for (Map.Entry<FlowController, Thread> entry : controllers.entrySet()) {
                FlowController ctrl = entry.getKey();
                clog.append(String.format("\n    %s", ctrl.report()));
            }
            clog.append("\n\nCanaryControllers:");
            for (Map.Entry<CanaryController, Thread> entry : canaryControllers.entrySet()) {
                CanaryController ctrl = entry.getKey();
                clog.append(String.format("\n    %s", ctrl.report()));
            }
            clog.append("\n\nCDCControllers:");
            for (Map.Entry<CDCController, Thread> entry : cdcControllers.entrySet()) {
                CDCController ctrl = entry.getKey();
                clog.append(String.format("\n    %s", ctrl.report()));
            }
            return clog.toString();
        } else {
            return "FlowManager not initialized yet or not properly configured";
        }
    }

    public CanaryController getCanaryController() {
        if (canaryControllers.size() != 1) {
            throw new IllegalStateException("Expected exactly one CanaryController, but found: " + canaryControllers.size());
        }
        return canaryControllers.keySet().iterator().next();
    }

    public CDCController getCDCController() {
        if (cdcControllers.size() != 1) {
            throw new IllegalStateException("Expected exactly one CDCController, but found: " + canaryControllers.size());
        }
        return cdcControllers.keySet().iterator().next();
    }

    public ConfigSettings getConfigSettings() {
        return configSettings;
    }
}
