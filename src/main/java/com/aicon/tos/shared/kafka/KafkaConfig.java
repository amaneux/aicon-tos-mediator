package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Configuration class for Kafka producers and consumers.
 * Provides methods to configure Kafka properties for producers and consumers,
 * including Avro serialization and deserialization.
 */
public class KafkaConfig {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConfig.class);

    public static final String aicon_dispatch_itv_job_resequence_request_topic = "aicon_dispatch_itv_job_resequence_request";
    public static final String aicon_dispatch_itv_job_resequence_response_topic = "aicon_dispatch_itv_job_resequence_response";

    public static final String aicon_dispatch_work_queue_activation_request_topic = "aicon_dispatch_work_queue_activation_request";
    public static final String aicon_dispatch_work_queue_activation_response_topic = "aicon_dispatch_work_queue_activation_response";

    public static final String AICON_TOS_CONNECTION_STATUS_TOPIC = "aicon_tos_connection_status";
    public static final String AICON_USER_CONTROL_TOPIC = "aicon_user_control";
    public static final String AICON_TOS_CONTROL_TOPIC = "aicon_tos_control";
    public static final String AICON_TOS_CONTROL_RULE_TABLE_TOPIC = "aicon_tos_control_rule_table";

//    public static final String AICON_TOS_CONNECT_REQUEST_TOPIC = aicon_dispatch_itv_job_resequence_request_topic;
//    public static final String AICON_TOS_CONNECT_RESPONSE_TOPIC = aicon_dispatch_itv_job_resequence_response_topic;

    public static final int POLL_WAIT_IN_MSEC = 2000;
    public static final int RETRY_POLL_WAIT_IN_MSEC = 5000;
    public static final String DEFAULT_AUTO_OFFSET_RESET = "latest";

    private static String schemaRegistryUrl;

    private KafkaConfig() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Returns the base properties used for both Kafka consumers and producers.
     * Includes configurations such as bootstrap servers and schema registry URL.
     *
     * @return A {@link Properties} object containing base Kafka properties.
     */
    public static Properties getBaseProps(String differentGroupId) {
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup kafkaConfig = config.getMainGroup(ConfigType.Connections).getChildGroup(ConfigType.Kafka);

        Properties props = new Properties();
        String host;
        String port;
        String schemaPort;
        String groupId = "aicon-tos-mediator";
        String bootstrapServers;
        if (kafkaConfig != null) {
            host = kafkaConfig.getItemValue(ConfigSettings.CFG_KAFKA_HOST);
            port = kafkaConfig.getItemValue(ConfigSettings.CFG_KAFKA_PORT);
            bootstrapServers = Arrays.stream(port.split(","))
                    .map(s -> host + ":" + s)
                    .collect(Collectors.joining(","));
            schemaPort = kafkaConfig.getItemValue(ConfigSettings.CFG_KAFKA_SCHEMA_REGISTRY_PORT);
            groupId = kafkaConfig.getItemValue(ConfigSettings.CFG_KAFKA_GROUP_ID, groupId);
            if (differentGroupId != null) {
                groupId = differentGroupId;
            }
        } else {
            // not sure if we need to keep the defaults or return null
            host = "localhost";
            port = "9092";
            bootstrapServers = String.format("%s:%s", host, port);
            schemaPort = "8081";
            if (LOG.isInfoEnabled()) {
                LOG.info(String.format("No Kafka config found, using default: host=%s, port=%s, schemaPort=%s ", host, port,
                        schemaPort));
            }
        }

        schemaRegistryUrl = String.format("http://%s:%s", host, schemaPort);

        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, "false");

        return props;
    }

    public static String getSchemaRegistryUrl() {
        if (schemaRegistryUrl == null) {
            getBaseProps();
        }
        return schemaRegistryUrl;
    }

    public static Properties getBaseProps() {
        return getBaseProps(null);
    }

    /**
     * Returns the properties used for configuring a Kafka consumer.
     * Includes deserializer configurations for keys and values, and consumer group ID.
     *
     * @return A {@link Properties} object containing Kafka consumer properties.
     */
    public static Properties getConsumerProps(String differentGroupId) {
        Properties props = getBaseProps(differentGroupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
        props.put("specific.avro.reader", "true"); // Ensures specific Avro classes are used
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, DEFAULT_AUTO_OFFSET_RESET);

        return props;
    }

    public static Properties getConsumerProps() {
        return getConsumerProps(null);
    }

    /**
     * Returns the properties used for configuring a Kafka producer.
     * Includes serializer configurations for keys and values.
     *
     * @return A {@link Properties} object containing Kafka producer properties.
     */
    public static Properties getProducerProps() {
        Properties props = getBaseProps();
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        props.put(ProducerConfig.RETRIES_CONFIG, "2");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "500");
        props.put("auto.register.schemas", "true");

        return props;
    }

    /**
     * Utility method to print Kafka configuration properties to the logger.
     */
    public static void logConfig() {
        if (LOG.isDebugEnabled()) {
            StringBuilder builder = new StringBuilder();

            builder.append("Kafka Producer Configuration:\n");
            getProducerProps().forEach((key, value) -> builder.append("  ").append(key).append(" = ").append(value).append("\n"));

            builder.append("Kafka Consumer Configuration:\n");
            getConsumerProps().forEach((key, value) -> builder.append("  ").append(key).append(" = ").append(value).append("\n"));

            LOG.debug(builder.toString());
        }
    }

    public static void logProperties(String title, String topic, Properties props) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Kafka {} {} properties:", title, topic);
            for (String key : props.stringPropertyNames()) {
                LOG.debug("  {}/{}: {}", topic, key, props.getProperty(key));
            }
        }
    }

    public static String getFlowRequestTopic(String flowId) {
        return getFlowTopic(flowId,"topic.source");
    }

    static String getFlowResponseTopic(String flowId) {
        return getFlowTopic(flowId,"topic.response.ok");
    }

    static String getFlowTopic(String flowId, String topicType) {
        ConfigSettings config = ConfigSettings.getInstance();
        if (ConfigSettings.getInstance().hasStorageError()) {
            return null;
        }
        ConfigGroup flowConfigs = config.getMainGroup(ConfigType.Flows);

        return flowConfigs.getChildGroup(ConfigType.Flow, flowId)
                .getItemValue(topicType);
    }
}
