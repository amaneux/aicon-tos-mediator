package com.aicon.tos;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.avlino.common.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class which has knowledge of the configuration content. The ConfigSettings should NOT have any knowledge
 * of its contents, so use this class to provide methods which has knowledge about the configuration files content.
 * todo yusuf make maximum use of this class, do not have local copies of config settings in users of the config, otherwise we will never be able to chnage config on the fly.
 */
public class ConfigDomain {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigDomain.class);

    // todo most of these keys are already available in the {@link generated.ConfigItemKeyEnum}, use them not copy them again here!
    public static final String CFG_ENVIRONMENT_NAME = "environment.name";
    public static final String CFG_TERMINAL_NAME = "terminal.name";
    public static final String CFG_YARD_SCOPE = "yard.scope";
    public static final String CFG_CANARY_INTERVAL_MS = "canary.interval.ms";
    public static final String CFG_CANARY_CDC_FREQUENCY = "canary.cdc.frequency";
    public static final String CFG_CANARY_ON_OFF = "canary.on.off";
    public static final String CFG_CDC_PREFIX = "cdc.topic.prefix";
    public static final String CFG_CONNECTION_TIMEOUT_MS = "connection.timeout.ms";
    public static final String CFG_TOS_POSITION_SCHEMA = "tos.position.schema";

    public static final String CFG_FLOW_URL_PATH = "url.path";
    public static final String CFG_FLOW_TOPIC_SOURCE = "topic.source";
    public static final String CFG_FLOW_TRANSFORMER_CLASS = "transformer.class";
    public static final String CFG_FLOW_TRANSFORMER_REQUEST = "transformer.request";
    public static final String CFG_FLOW_TRANSFORMER_RESPONSE = "transformer.response";
    public static final String CFG_FLOW_TOPIC_RESPONSE_OK = "topic.response.ok";
    public static final String CFG_FLOW_TOPIC_RESPONSE_FAIL = "topic.response.fail";

    public static final String CFG_CDC_TOPIC_NAME = "topic.name";
    public static final String CFG_CDC_GROUP_ID = "group.id";
    public static final String CFG_CDC_THRESHOLD = "cdc.threshold";

    public static final String SCOPE_SEP = Constants.REGEX_SLASH;
    public static final String SCOPE_ID_OPERATOR = "operator.id";        // == our customer id
    public static final String SCOPE_ID_COMPLEX = "complex.id";         // the geograpic area like Rotterdam, LongBeach, etc
    public static final String SCOPE_ID_FACILITY = "facility.id";        // == our terminal id
    public static final String SCOPE_ID_YARD = "yard.id";

    private static String topicPrefix = null;

    private ConfigDomain() {
        throw new IllegalStateException("Utility class");
    }

    public static String getScopePart(String scopeKey) {
        return splitScope().get(scopeKey);
    }

    public static Map<String, String> splitScope() {
        String scope = getGeneralItem(CFG_YARD_SCOPE);
        Map<String, String> map = new LinkedHashMap<>(4);
        if (scope != null) {
            String[] parts = scope.split(SCOPE_SEP);
            if (parts.length == 4) {
                map.put(SCOPE_ID_OPERATOR, parts[0]);
                map.put(SCOPE_ID_COMPLEX, parts[1]);
                map.put(SCOPE_ID_FACILITY, parts[2]);
                map.put(SCOPE_ID_YARD, parts[3]);
            } else {
                LOG.error("Scope {} is not build up as expected: operator/complex/facility/yard", scope);
            }
        }
        return map;
    }

    public static String getGeneralItem(String itemKey) {
        try {
            return ConfigSettings.getInstance().getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems)
                    .findItem(itemKey).value();
        } catch (Exception e) {
            LOG.error("Config item {}/{}.{} not found.", ConfigType.General, ConfigType.GeneralItems, itemKey);
            return null;
        }
    }

    public static String getPrefixedTopic(ConfigGroup configGroup, String itemName) {
        String topicName = configGroup.getItemValue(itemName);
        return topicName == null ? null : prefixIfNeeded(topicName);
    }

    /**
     * Adds a prefix to the provided topic name if it doesn't already have at least
     * one period. This method ensures that the topic name conforms to a
     * predefined structure by appending a prefix retrieved from the application's
     * configuration.
     *
     * @param topicName the original topic name, which may or may not require a prefix
     * @return the resulting topic name with a prefix added, if necessary; if the topic
     *         name already meets the required structure (having at least one period),
     *         it will be returned unchanged
     */
    public static String prefixIfNeeded(String topicName) {
        return topicName != null && topicName.chars().filter(c -> c == '.').count() >= 1
                ? topicName
                : getTopicPrefix() + topicName;
    }

    public static String getTopicPrefix() {
        try {
            if (topicPrefix == null) {
                topicPrefix = ConfigSettings.getInstance()
                        .getMainGroup(ConfigType.Connections)
                        .getChildGroup(ConfigType.Kafka)
                        .getItemValue(ConfigSettings.CFG_CDC_PREFIX);
            }
            return topicPrefix;
        } catch (Exception e) {
            LOG.error("Config item {}/{}.{} not found.", ConfigType.Connections, ConfigType.Kafka, CFG_CDC_PREFIX);
            return null;
        }
    }

    public static void resetTopicPrefixForTests() {
        topicPrefix = null; // Reset the field
    }
}

