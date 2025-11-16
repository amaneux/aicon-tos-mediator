package com.aicon.tos.interceptor;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import generated.ConfigItemKeyEnum;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InterceptorConfig {

    private static final String FALSE = String.valueOf(false);

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorConfig.class);
    private final Map<String, InterceptorEntityConfig> entityConfig = new HashMap<>();
    private boolean useMockedConsumers = false;

    public InterceptorConfig() {

        ConfigSettings configSettings = ConfigSettings.getInstance();
        ConfigGroup interceptorConfigGroup = configSettings.getMainGroup(ConfigType.Interceptors);

        convertConfigGroupToInterceptorEntityConfig(interceptorConfigGroup);
    }

    public InterceptorConfig(ConfigGroup interceptorConfigGroup) {
        convertConfigGroupToInterceptorEntityConfig(interceptorConfigGroup);
    }

    private void convertConfigGroupToInterceptorEntityConfig(ConfigGroup interceptorConfigGroup) {
        if (interceptorConfigGroup == null || !interceptorConfigGroup.isOfType(ConfigType.Interceptors)) {
            LOG.warn("Invalid or null ConfigGroup provided. No topics loaded.");
            return;
        }

        LOG.info("Initializing Interceptor configuration from entities...");
        for (ConfigGroup topicGroup : interceptorConfigGroup.getChildren()) {
            if (topicGroup.isOfType(ConfigType.InterceptorEntity)) {
                try {
                    InterceptorEntityConfig entityConfig = createInterceptorEntityConfig(topicGroup);
                    this.entityConfig.put(entityConfig.getEntityName(), entityConfig);
                    LOG.info("Added configuration for topic '{}'", entityConfig.getTopicName());
                } catch (IllegalArgumentException e) {
                    LOG.error("Failed to process topic configuration for group '{}': {}", topicGroup.getName(), e.getMessage());
                }
            }
        }
    }

    private InterceptorEntityConfig createInterceptorEntityConfig(ConfigGroup entityConfig) {
        String entityConfigName = entityConfig.getName();
        if (entityConfigName == null || entityConfigName.isEmpty()) {
            throw new IllegalArgumentException("Topic name is missing or empty");
        }

        // Extract configuration properties with defaults
        String entityName = entityConfig.getName();
        String topicName = entityConfig.getItemValue(ConfigItemKeyEnum.TOPIC_NAME.value());
        String groupId = entityConfig.getItemValue(ConfigItemKeyEnum.GROUP_ID.value());
        int maxMessages = Integer.parseInt(entityConfig.getItemValue("max.messages.in.storage", "10000"));
        Duration maxTime = Duration.ofMinutes(Long.parseLong(entityConfig.getItemValue("max.time.in.storage", "30")));
        long processingDelay = Long.parseLong(entityConfig.getItemValue("processing.delay", "2000"));
        boolean testCreations = Boolean.parseBoolean(entityConfig.getItemValue("test.creations", FALSE));
        boolean testChanges = Boolean.parseBoolean(entityConfig.getItemValue("test.changes", FALSE));
        boolean testDeletions = Boolean.parseBoolean(entityConfig.getItemValue("test.deletions", FALSE));

        ConfigGroup scenariosGroup = entityConfig.getChildGroup(ConfigType.Scenarios);

        return new InterceptorEntityConfig(
                entityName,
                topicName,
                groupId,
                maxMessages,
                maxTime,
                processingDelay,
                scenariosGroup,
                testCreations,
                testChanges,
                testDeletions
        );
    }

    public List<String> getEntityKeys() {
        return List.copyOf(entityConfig.keySet());
    }

    public Collection<InterceptorEntityConfig> getEntities() {
        return List.copyOf(entityConfig.values());
    }

    public InterceptorEntityConfig getEntityConfig(String entity) {
        return entityConfig.get(entity);
    }

    /**
     * Retrieves the schema for a topic.
     *
     * @param topic The name of the topic
     * @return Parsed Avro schema
     */
    public Schema getSchema(String topic) {
        String schemaJsonName = "";
        try {
            schemaJsonName = "topic_schemas/" + topic + ".json";
            InputStream input = InterceptorConfig.class.getClassLoader().getResourceAsStream(schemaJsonName);
            if (input == null) {
                throw new RuntimeException(schemaJsonName + " not found in resources.");
            }
            String schemaJson = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            return new Schema.Parser().parse(schemaJson);
        } catch (IOException e) {
            throw new RuntimeException(schemaJsonName + " not read and parsed.");
        }
    }

    public boolean usesMockedConsumers() {
        return this.useMockedConsumers;
    }

    public void clearEntityConfigs() {
        this.entityConfig.clear();
    }

    public void addEntityConfig(String entityName, InterceptorEntityConfig interceptorEntityConfig) {
        interceptorEntityConfig.setUseMockedInterceptor(this.useMockedConsumers);
        this.entityConfig.put(entityName, interceptorEntityConfig);
    }

    public void clearEntities() {
        this.entityConfig.clear();
    }

    public void printAllConfigs() {
        entityConfig.forEach((name, config) ->
                LOG.info("Topic '{}': {}", name, config));
    }
}