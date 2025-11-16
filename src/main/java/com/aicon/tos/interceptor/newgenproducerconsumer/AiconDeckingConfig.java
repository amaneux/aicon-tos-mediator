package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;

public class AiconDeckingConfig {
    private static String aiconDeckingRequestTopic;
    private static String aiconDeckingResponseTopic;
    private static ConfigGroup generalItems = null;
    private static ConfigSettings configSettings;

    private static String configfileWithoutTomcat = "C:\\projects\\tools\\apache-tomcat-10.1.39\\conf\\mediator\\DCT-DEV-aicon-connections.xml";

    AiconDeckingConfig() {
        getConfigItems();

        if (aiconDeckingRequestTopic == null || aiconDeckingResponseTopic == null) {
            throw new RuntimeException("Missing configuration for Kafka topic(s): " + aiconDeckingRequestTopic + ", " + aiconDeckingResponseTopic);
        }
        if (aiconDeckingRequestTopic.equals(aiconDeckingResponseTopic)) {
            throw new RuntimeException("Request and response topics must be different: " + aiconDeckingRequestTopic);
        }
        if (aiconDeckingRequestTopic.isEmpty() || aiconDeckingResponseTopic.isEmpty()) {
        }
    }

    private static void getConfigItems() {
        if (generalItems == null) {
            configSettings = ConfigSettings.getInstance();
            if (configSettings.getRoot() == null) {
                // Assume we run without Tomcat, so we need to set the config file explicitly.
                ConfigSettings.setConfigFile(configfileWithoutTomcat);
                configSettings = ConfigSettings.getInstance();
            }
            generalItems = configSettings.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
            aiconDeckingRequestTopic = generalItems.getItemValue("decking.engine.request.topic");
            aiconDeckingResponseTopic = generalItems.getItemValue("decking.engine.response.topic");
        }
    }

    public static String getAiconDeckingRequestTopic() {
        getConfigItems();
        return aiconDeckingRequestTopic;
    }

    public static String getAiconDeckingResponseTopic() {
        getConfigItems();
        return aiconDeckingResponseTopic;
    }
}
