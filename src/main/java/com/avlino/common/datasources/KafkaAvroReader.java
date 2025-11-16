package com.avlino.common.datasources;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.micrometer.common.util.StringUtils;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;

import java.net.InetAddress;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.apache.kafka.clients.consumer.ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.DEFAULT_API_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;

public class KafkaAvroReader {
    private static final Logger log           = Logger.getLogger(KafkaAvroReader.class.getName());

    public static final String CFG_KAFKA_HOST = "hostname";
    public static final String CFG_KAFKA_PORT = "hostport";
    public static final String CFG_SCHEMA_REGISTRY_PORT = "schema.registry.port";
    public static final String CFG_MAX_SEARCH_TIME_S = "max.search.time.s";
    public static final String CFG_CONNECTION_TIMEOUT_MS = "connection.timeout.ms";
    public static final String CFG_POLL_TIMEOUT_MS = "poll.timeout.ms";

    //private static final org.slf4j.Logger LOG = LoggerFactory.getLogger(KafkaAvroReader.class);
    private static DateTimeFormatter formatFullTs       = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm.ss").withZone(ZoneId.systemDefault());
    private static DateTimeFormatter formatFullTsWithMs = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm.ss.SSS").withZone(ZoneId.of("-01:00"));

    private static final String FIND_ALL = "%";

    private long connectionTimeoutMs = 5000L;
    private long searchTimeS = 10;
    private long pollTimeoutMs = 1000;
    private int kafkaPort =  9092;
    private int schemaRegistryPort = 8081;
    private String cdcPrefix = "tos.";
    private String kafkaGroupId = null;     //"aicon-research-app";

    long matchesLeft;

    long firstOffset = 0;
    long lastOffset;
    long currentOffset = 0;
    private KafkaConsumer<GenericRecord, GenericRecord> consumer = null;
    private TopicPartition partition = null;


    public static void main(String[] args) {
        KafkaAvroReader reader = new KafkaAvroReader();

        List<ConsumerRecord<GenericRecord, GenericRecord>> records;

        try {
            records = reader.setSearchTimeMs(10000)
                    .searchInTopicKey(
                    "10.50.3.4",
                    "tos.apex.dbo.inv_wi",
                    null,
                    null,
                    2463570L,
                    1L
            );

//        records = reader.retrieveAllFromTopic(
//                "dct-dev-kafka.avlino.az",
//                "tos.apex.dbo.road_gates"
//        );

            for (ConsumerRecord<GenericRecord, GenericRecord> record: records) {
                Instant instant = Instant.ofEpochMilli(record.timestamp());
                String topicTs = formatFullTsWithMs.format(instant);
                System.out.println(String.format("Found record@%s, offset=%s: %s",
                        topicTs, record.offset(), GenericData.get().toString(record.value())));
//            printFieldsRecursive(record.value(), "");
            }
        } catch (Exception exc) {
            System.out.println("Exception occurred: " + exc);
        }
    }

    public List<ConsumerRecord<GenericRecord, GenericRecord>> searchInTopicKey(
            String kafkaHost,
            String topic,
            String fieldName,
            String fieldValue,
            Long offsetStart,
            Long offsetLimit
    ) throws Exception {
        List<ConsumerRecord<GenericRecord, GenericRecord>> result = new ArrayList<>();
        try {
            long tStart = System.currentTimeMillis();
            prepareSearch(kafkaHost, topic, offsetStart, offsetLimit);
            log.info(String.format("%s records in this topic", getRecordCount()));
            do {
                List<ConsumerRecord<GenericRecord, GenericRecord>> records = search(fieldName, fieldValue);
                result.addAll(records);
                if (log.getLevel().equals(Level.FINER)) {
                    log.finer(String.format(" Found %s records in %s ms\n%s",
                            records.size(), (System.currentTimeMillis() - tStart), records));
                }
            } while (!searchEnded());
            closeSearch();
        } catch (Exception exc) {
            throw exc;
        }
        return result;
    }


    /**
     * Prepare a new search, to be called before the actual {@link #search} can be executed.
     * @param kafkaHost the kafka host to connect to
     * @param searchTopic the (partial) topic name to search for
     * @throws ExecutionException something went wrong while retrieving the topic list
     */
    public List<String> listTopics(
            String kafkaHost,
            String searchTopic
    ) throws ExecutionException {
        log.info(String.format("listTopics: host=%s, topic=%s", kafkaHost, searchTopic));

        // Kafka consumer properties
        Properties props = config(kafkaHost);

        try (AdminClient adminClient = AdminClient.create(props)) {
            Set<String> topics = adminClient.listTopics().names().get(); // Fetch all topic names

            String lcTopic = searchTopic.toLowerCase();
            // Perform a case-insensitive partial search
            List<String> matchingTopics = topics.stream()
                    .filter(topic -> FIND_ALL.equals(lcTopic) || topic.toLowerCase().contains(lcTopic))
                    .collect(Collectors.toList());
            return matchingTopics;
        } catch (InterruptedException | ExecutionException e) {
            String text = String.format("Search for topic %s failed, reason: %s", searchTopic, e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            log.severe(text);
            throw new ExecutionException(text, null);
        }
    }

    /**
     * Prepare a new search, to be called before the actual {@link #search} can be executed.
     * @param kafkaHost the kafka host to connect to
     * @param topic the topic to search in
     * @param offsetStart where to start the search
     * @param offsetLimit limits the number of records to retrieve
     * @throws Exception
     */
    public void prepareSearch(
        String kafkaHost,
        String topic,
        Long offsetStart,
        Long offsetLimit
    ) throws Exception {
        log.info(String.format("PrepareSearch: host=%s, topic=%s, offset-start=%s, -limit=%s, read-time=%s ms, poll-timeout %s ms",
                kafkaHost, topic, offsetStart, offsetLimit, getSearchTimeS(), getPollTimeoutMs()));

        matchesLeft = offsetLimit != null ? offsetLimit : 500;      // offsetReads counts down until 0
        firstOffset = -1;
        currentOffset = 0;

        // Kafka consumer properties
        Properties props = config(kafkaHost);

        // Create Kafka consumer
        closeSearch();
        log.info(String.format("Connecting to kafka %s ...", props.get(BOOTSTRAP_SERVERS_CONFIG)));
        consumer = new KafkaConsumer<>(props);

        long tStart = System.currentTimeMillis();
        try {
            partition = new TopicPartition(topic, 0);
            List<TopicPartition> partitions = Arrays.asList(partition);
            consumer.assign(partitions);
            consumer.seekToEnd(partitions);
            lastOffset = consumer.position(partition) - 1;      // offset starts with 0
            long beginAt = offsetStart != null ? offsetStart : 0;
            if (beginAt < 0) {
                beginAt = Math.max(lastOffset + (beginAt + 1), 0);
            }
            consumer.seek(partition, beginAt);
            log.info(String.format("PrepareSearch: offset beginAt=%s, highest=%s", beginAt, lastOffset));
        } catch (Exception exc) {
            log.severe("Kafka reader failed, reason: " + exc);
            closeSearch();
            throw exc;
        } finally {
            long timeUsed = System.currentTimeMillis() - tStart;
            log.info(String.format("Connecting took %s ms",(timeUsed)));
            searchTimeS -= timeUsed;
        }
    }

    /**
     * Search for the records of interest, {@code pollTimeoutMs} per cycle.
     * Call this method repeatedly until searchEnded() returns false.
     * @param fieldName the name to search in (when null will be the first field)
     * @param fieldValues a , separated list of values to search for
     * @return all matching records or else an empty list
     */
    public List<ConsumerRecord<GenericRecord, GenericRecord>> search(
            String fieldName,
            String fieldValues
    ) {
        long tStart = System.currentTimeMillis();
        List<ConsumerRecord<GenericRecord, GenericRecord>> result = new ArrayList<>();
        try {
            // read no. of records per 1000 ms
            ConsumerRecords<GenericRecord, GenericRecord> records =
                    consumer.poll(Duration.ofMillis(Math.min(pollTimeoutMs, searchTimeS)));

            int matched = 0;
            Set<String> matchSet = null;
            if (!StringUtils.isEmpty(fieldValues)) {
                matchSet = new HashSet<>(Arrays.asList(fieldValues.split("[,]")));
            }

            for (ConsumerRecord<GenericRecord, GenericRecord> record : records) {
                Object value;
                if (matchSet == null) {
                    result.add(record);
                    matchesLeft--;
                } else {
                    if (StringUtils.isEmpty(fieldName)) {
                        value = record.key().get(0);
                    } else {
                        // when a field is given, we must look it up in the value of the record
                        value = ((GenericRecord)record.value().get(1)).get(fieldName);
                    }
                    if (matchSet.contains(String.valueOf(value))) {
                        result.add(record);
                        matched++;
                        matchesLeft--;
                    }
                }
                if (result.size() == 1) {
                    firstOffset = record.offset();
                }
                if (record.offset() >= lastOffset) {     // reached last record
                    stop();
                }
                currentOffset = record.offset();
                if (searchEnded()) {
                    closeSearch();
                    break;
                }
            }
            log.info(String.format("Records@offset %10d, matched %4d from %4d",
                    getCurrentOffset(), matched, records.count()));
        } catch (Exception exc) {
            log.severe("Kafka reader failed, reason: " + exc);
        } finally {
            searchTimeS -= (System.currentTimeMillis() - tStart);
        }
        return result;
    }


    public void stop() {
        matchesLeft = 0;
    }


    /**
     * Checks if search has ended (reached end of stream/read-limit/search-time)
     * @return when true end has reached
     */
    public boolean searchEnded() {
        return matchesLeft <= 0 || searchTimeS <= 0;
    }


    /**
     * Closes the consumer explicitly (a new search will close the previous search automatically).
     */
    public void closeSearch() {
        if (consumer != null) {
            consumer.unsubscribe();
            consumer.close();
            consumer = null;
        }
    }


    public List<ConsumerRecord<GenericRecord, GenericRecord>> searchInTopicKey(
            String kafkaHost,
            String topic,
            String fieldName,
            String fieldValue
    ) throws Exception {
        return searchInTopicKey(kafkaHost, topic, fieldName, fieldValue, null, null);
    }


    public List<ConsumerRecord<GenericRecord, GenericRecord>> retrieveFromTopic (
            String kafkaHost,
            String topic,
            long offsetStart,
            long offsetGet
    ) throws Exception {
        return searchInTopicKey(kafkaHost, topic, null, null, offsetStart, offsetGet);
    }


    public List<ConsumerRecord<GenericRecord, GenericRecord>> retrieveAllFromTopic (
            String kafkaHost,
            String topic
    ) throws Exception {
        return searchInTopicKey(kafkaHost, topic, null, null, null, null);
    }


    public void tail (
            String kafkaHost,
            String topic
    ) {
        // Kafka consumer properties
        Properties props = config(kafkaHost);

        // Subscribe to Kafka topic
        KafkaConsumer<String, GenericRecord> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singletonList(topic));

        // Start consuming future messages
        while (true) {
            for (ConsumerRecord<String, GenericRecord> record : consumer.poll(Duration.ofMillis(Long.MAX_VALUE))) {
                // Process the record
                log.info(String.format("Received message: key=%s, value=%s, offset=%d%n", record.key(), record.value(), record.offset()));
            }
        }
    }

    private Properties config (
            String kafkaHost
    ) {
        String[] ipAddress = kafkaHost.split("[:]");
        try {
            // somehow kafka doesn't like the .avlino.az always (due to VPN issues), so  we translate it here.
            ipAddress[0] = InetAddress.getByName(ipAddress[0]).getHostAddress();
        } catch(Exception exc) {
            log.severe(String.format("Can't translate IP-address, reason: %s", exc.getMessage()));
        }

        // Kafka consumer properties
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, String.format("%s:%s", ipAddress[0], ipAddress.length > 1 ? ipAddress[1] : kafkaPort));
        if (kafkaGroupId != null) {
            props.put(GROUP_ID_CONFIG, kafkaGroupId);
        }
        props.put(KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());                    // needed for cdc topics
        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class.getName());
//        props.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());                     // needed for cdc topics
//        props.put(VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put("schema.registry.url", String.format("http://%s:%s", ipAddress[0], schemaRegistryPort));  // URL of the Schema Registry
        props.put("specific.avro.reader", "false");                                                         // Use specific record or not
        props.put(DEFAULT_API_TIMEOUT_MS_CONFIG, String.valueOf(connectionTimeoutMs));
        props.put(REQUEST_TIMEOUT_MS_CONFIG, String.valueOf(connectionTimeoutMs));
        return props;
    }

    private void printFieldsRecursive(GenericRecord rec, String prefix) {
        List<String> keys = rec.getSchema().getFields().stream().map(Schema.Field::name).toList();

        for (String key: keys) {
            Object obj = rec.get(key);
            if (obj instanceof GenericRecord) {
                printFieldsRecursive((GenericRecord)obj, prefix + key + ".");
            } else {
                log.info(String.format("%s%s = %s", prefix, key, rec.get(key)));
            }
        }
    }

    // Configuration setters returns this object to allow repeated setters to be called.

    public long getSearchTimeS() {
        return searchTimeS;
    }

    public KafkaAvroReader setSearchTimeMs(long timeMs) {
        this.searchTimeS = timeMs;
        return this;
    }

    public long getConnectionTimeoutMs() {
        return connectionTimeoutMs;
    }

    public void setConnectionTimeoutMs(long connectionTimeoutMs) {
        this.connectionTimeoutMs = connectionTimeoutMs;
    }

    public long getPollTimeoutMs() {
        return pollTimeoutMs;
    }

    public KafkaAvroReader setPollTimeoutMs(long pollTimeoutMs) {
        this.pollTimeoutMs = pollTimeoutMs;
        return this;
    }

    public int getKafkaPort() {
        return kafkaPort;
    }

    public KafkaAvroReader setKafkaPort(int kafkaPort) {
        this.kafkaPort = kafkaPort;
        return this;
    }

    public int getSchemaRegistryPort() {
        return schemaRegistryPort;
    }

    public KafkaAvroReader setSchemaRegistryPort(int schemaRegistryPort) {
        this.schemaRegistryPort = schemaRegistryPort;
        return this;
    }

    public String getCdcPrefix() {
        return cdcPrefix;
    }

    public KafkaAvroReader setCdcPrefix(String cdcPrefix) {
        this.cdcPrefix = cdcPrefix;
        return this;
    }

    public String getKafkaGroupId() {
        return kafkaGroupId;
    }

    public KafkaAvroReader setKafkaGroupId(String kafkaGroupId) {
        this.kafkaGroupId = kafkaGroupId;
        return this;
    }

    // Monitoring getters

    public long getFirstOffset() {
        return firstOffset;
    }

    public long getLastOffset() {
        return lastOffset;
    }

    public long getCurrentOffset() { return currentOffset; }

    public long getRecordCount() {
        return lastOffset + 1;
    }
}
