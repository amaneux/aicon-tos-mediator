package com.aicon.tos.shared.kafka;

import ch.qos.logback.classic.Level;
import com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader;
import com.aicon.tos.shared.config.ConfigSettings;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.config.ConfigResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import static com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader.client;

/**
 * Utility class for creating an identical clone of an existing Kafka topic.
 * This class creates a new topic with the same configuration (partitions, replication factor,
 * and topic configs) as the source topic, and registers the same schema in the schema registry.
 */
public class TopicCloner {

    private static final Logger LOG = LoggerFactory.getLogger(TopicCloner.class);

    private final AdminClient adminClient;

    /**
     * Creates a new TopicCloner instance.
     *
     * @param bootstrapServers the Kafka bootstrap servers address
     */
    public TopicCloner(String bootstrapServers) {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Suppress Kafka client internal logging
        props.put("log4j.logger.org.apache.kafka", Level.ERROR);
        props.put("log4j.logger.kafka", Level.ERROR);
        props.put("log4j.logger.org.apache.kafka.clients.NetworkClient", Level.ERROR);

        this.adminClient = AdminClient.create(props);
    }

    /**
     * Creates a clone of the source topic with the specified new name.
     * The new topic will have the same number of partitions, replication factor,
     * configuration, and schema as the source topic.
     *
     * @param sourceTopic  the name of the source topic to clone
     * @param newTopicName the name of the new topic to create
     * @throws ExecutionException   if the execution fails
     * @throws InterruptedException if the thread is interrupted
     * @throws IOException          if an I/O error occurs
     * @throws RestClientException  if a schema registry error occurs
     */
    public String cloneTopic(String sourceTopic, String newTopicName)
            throws ExecutionException, InterruptedException, IOException, RestClientException {

        LOG.info("Starting to clone topic '{}' as '{}'", sourceTopic, newTopicName);

        // Check if source topic exists
        if (!topicExists(sourceTopic)) {
            throw new IllegalArgumentException("Source topic '" + sourceTopic + "' does not exist");
        }

        // Check if new topic already exists
        if (topicExists(newTopicName)) {
            throw new IllegalArgumentException("Topic '" + newTopicName + "' already exists");
        }

        // Get source topic description
        TopicDescription sourceDescription = getTopicDescription(sourceTopic);
        int numPartitions = sourceDescription.partitions().size();
        short replicationFactor = (short) sourceDescription.partitions().get(0).replicas().size();

        LOG.info("Source topic has {} partitions and replication factor {}",
                numPartitions, replicationFactor);

        // Get source topic configuration
        Map<String, String> sourceConfig = getTopicConfig(sourceTopic);
        LOG.info("Retrieved {} configuration entries from source topic", sourceConfig.size());

        // Create new topic with same configuration
        createTopic(newTopicName, numPartitions, replicationFactor, sourceConfig);

        // Clone schema from schema registry
        cloneSchema(sourceTopic, newTopicName);

        LOG.info("Successfully cloned topic '{}' as '{}'", sourceTopic, newTopicName);

        return newTopicName;
    }

    /**
     * Creates a clone of the source topic with a suffix added to the source topic name.
     *
     * @param sourceTopic the name of the source topic to clone
     * @param suffix      the suffix to add to the topic name (e.g., "_tst")
     * @throws ExecutionException   if the execution fails
     * @throws InterruptedException if the thread is interrupted
     * @throws IOException          if an I/O error occurs
     * @throws RestClientException  if a schema registry error occurs
     */
    public String cloneTopicWithSuffix(String sourceTopic, String suffix)
            throws ExecutionException, InterruptedException, IOException, RestClientException {
        String newTopicName = sourceTopic + suffix;
        return cloneTopic(sourceTopic, newTopicName);
    }

    public boolean topicExists(String topicName) throws ExecutionException, InterruptedException {
        LOG.info("Checking if topic '{}' exists", topicName);
//        LOG.info("Topics: {}", adminClient.listTopics().names().get());

        Set<String> topics = adminClient.listTopics().names().get();
        return topics.contains(topicName);
    }

    private TopicDescription getTopicDescription(String topicName)
            throws ExecutionException, InterruptedException {
        DescribeTopicsResult result = adminClient.describeTopics(Collections.singletonList(topicName));
        return result.topicNameValues().get(topicName).get();
    }

    private Map<String, String> getTopicConfig(String topicName)
            throws ExecutionException, InterruptedException {
        ConfigResource resource = new ConfigResource(ConfigResource.Type.TOPIC, topicName);
        DescribeConfigsResult result = adminClient.describeConfigs(Collections.singletonList(resource));
        Config config = result.all().get().get(resource);

        return config.entries().stream()
                .filter(entry -> entry.source() != ConfigEntry.ConfigSource.DEFAULT_CONFIG
                        && entry.source() != ConfigEntry.ConfigSource.STATIC_BROKER_CONFIG)
                .collect(Collectors.toMap(ConfigEntry::name, ConfigEntry::value));
    }

    private void createTopic(String topicName, int numPartitions, short replicationFactor,
                             Map<String, String> config)
            throws ExecutionException, InterruptedException {
        NewTopic newTopic = new NewTopic(topicName, numPartitions, replicationFactor);
        newTopic.configs(config);

        CreateTopicsResult result = adminClient.createTopics(Collections.singletonList(newTopic));
        result.all().get();

        LOG.info("Created topic '{}' with {} partitions and replication factor {}",
                topicName, numPartitions, replicationFactor);
    }

    private void cloneSchema(String sourceTopic, String destinationTopic)
            throws IOException, RestClientException {

        String destinationSubject = destinationTopic + "-value";

        // Get schema from source topic
        Schema sourceSchema = SchemaLoader.getValueSchemaFromRegistry(sourceTopic);

        if (sourceSchema == null) {
            LOG.warn("No schema found for source topic '{}'. Skipping schema registration.", sourceTopic);
            return;
        }

        // Register schema for destination topic using ParsedSchema
        AvroSchema avroSchema = new AvroSchema(sourceSchema);
        int schemaId = client.register(destinationSubject, avroSchema);
        LOG.info("Registered schema for topic '{}' with ID: {}", destinationTopic, schemaId);

        // Also clone key schema if it exists
        try {
            String sourceKeySubject = sourceTopic + "-key";
            String schemaString = client.getLatestSchemaMetadata(sourceKeySubject).getSchema();
            if (schemaString != null) {
                Schema keySchema = new Schema.Parser().parse(schemaString);
                String destinationKeySubject = destinationTopic + "-key";
                AvroSchema avroKeySchema = new AvroSchema(keySchema);
                int keySchemaId = client.register(destinationKeySubject, avroKeySchema);
                LOG.info("Registered key schema for topic '{}' with ID: {}", destinationTopic, keySchemaId);
            }
        } catch (RestClientException e) {
            LOG.debug("No key schema found for source topic '{}'. This is normal for topics with string keys.", sourceTopic);
        }
    }

    /**
     * Closes the AdminClient and releases resources.
     */
    public void close() {
        if (adminClient != null) {
            adminClient.close();
            LOG.info("AdminClient closed");
        }
    }

    /**
     * Main method for testing or running the topic cloner from command line.
     */
    public static void main(String[] args) {

        ConfigSettings.setConfigFile("C:\\Users\\HarrieVanRijn\\IdeaProjects\\Aicon-Tos-Mediator\\src\\main\\resources\\conf\\mediator\\DCT-DEV-aicon-connections.xml");
        Properties properties = KafkaConfig.getConsumerProps();
        
        String bootstrapServers = properties.getProperty("bootstrap.servers");
        String sourceTopic = "tos.apex.dbo.inv_move_event";

        TopicCloner cloner = new TopicCloner(bootstrapServers);
        try {
            String newTopic = cloner.cloneTopicWithSuffix(sourceTopic, "_eli_tst");
            LOG.info("Created new topic '{}'", newTopic);
        } catch (Exception e) {
            LOG.error("Failed to clone topic", e);
            System.exit(1);
        } finally {
            cloner.close();
        }
    }
}
