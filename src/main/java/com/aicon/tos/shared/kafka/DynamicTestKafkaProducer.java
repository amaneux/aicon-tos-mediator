package com.aicon.tos.shared.kafka;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.interceptor.newgenproducerconsumer.SchemaFieldExtractor;
import com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.avlino.common.utils.CsvParser;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.aicon.tos.ConfigDomain.getPrefixedTopic;
import static com.aicon.tos.ConfigDomain.prefixIfNeeded;

/**
 * This class provides functionality for dynamically producing messages to Kafka topics.
 * It manages Kafka producers for different topics and supports sending generic Avro
 * records as messages to Kafka. Additionally, it provides utility methods for managing
 * generic Avro records and parsing data into schema-compatible formats.
 */
public class DynamicTestKafkaProducer {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicTestKafkaProducer.class);

    // Map to manage Kafka producers per topic
    private final Map<String, KafkaProducerBase<GenericRecord, GenericRecord>> producerMap = new ConcurrentHashMap<>();

    public boolean sendMessagToTestTopic(String topic, String testTopic, String[] before, String[] after) {
        topic = prefixIfNeeded(topic);
        testTopic = prefixIfNeeded(testTopic);

        KafkaProducerBase<GenericRecord, GenericRecord> producer = setUpTestTopic(topic, testTopic);

        // TODO because of LBCT does not permit to create schemas, reuse schema of original topic
        GenericRecord valueMessage = setUpValueGenericRecord(topic, before, after);
        GenericRecord keyMessage = setUpKeyGenericRecord(topic, getKey(before, after));
        sendMessage(testTopic, producer, keyMessage , valueMessage);

        return true;
    }

    private Long getKey(String[] before, String[] after) {
        if (before != null)
            return Long.parseLong(before[0]);

        if (after != null)
            return Long.parseLong(after[0]);

        LOG.warn("No key found because before and after are null");

        return null;
    }

    /**
     * Sends a message to a Kafka topic. If the producer for the topic does not already exist,
     * it creates a new producer and stores it in the map.
     *
     * @param topic The name of the Kafka topic.
     */
    private KafkaProducerBase<GenericRecord, GenericRecord> setUpTestTopic(String topic, String testTopic) {
        // Retrieve or create a producer for the specified topic
        return producerMap.computeIfAbsent(testTopic, t -> {
            TopicCloner topicCloner = new TopicCloner(KafkaConfig.getBaseProps().getProperty("bootstrap.servers"));
            try {
                if (!topicCloner.topicExists(t)) {
                    topicCloner.cloneTopic(topic, t);
                }

                LOG.info("Creating new producer for topic: {}", t);
                return new KafkaProducerBase<>(t, true) {
                    // Implementation can be added here if needed
                };
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Sends a message to a Kafka topic. If the producer for the topic does not already exist,
     * it creates a new producer and stores it in the map.
     *
     * @param topic   The name of the Kafka topic.
     * @param key     The key of the Kafka message.
     * @param message The message to send (must implement {@link GenericRecord}).
     */
    private void sendMessage(String topic, KafkaProducerBase<GenericRecord, GenericRecord> producer, GenericRecord key, GenericRecord message) {
        // Use the producer to send the message
        producer.sendMessage(key, message);
        LOG.info("Message sent to topic: {} with key: {}", topic, key);
    }

    /**
     * Closes all Kafka producers currently in use.
     */
    public void closeAllProducers() {
        producerMap.forEach((topic, producer) -> {
            // Close each producer and log the action
            producer.close();
            LOG.info("Closed producer for topic: {}", topic);
        });
        producerMap.clear(); // Optional: Clear the producer map after closing all producers
    }

    private GenericRecord setUpKeyGenericRecord(String topic, Long key) {
        Schema keySchema = SchemaLoader.getKeySchemaFromRegistry(topic);

        GenericRecord keyRecord = new GenericData.Record(keySchema);
        setField(keyRecord, "gkey", key);

        return keyRecord;
    }

    private GenericRecord setUpValueGenericRecord(String topic, String[] before, String[] after) {
        Schema afterSchema;
        Schema beforeSchema;
        Schema sourceSchema;
        Schema envelopeSchema = SchemaLoader.getValueSchemaFromRegistry(topic);

        afterSchema = envelopeSchema.getField("after").schema().getTypes().stream()
                .filter(s -> s.getType() == Schema.Type.RECORD).findFirst()
                .orElseThrow(() -> new RuntimeException("No record type found for 'after' field"));
        beforeSchema = envelopeSchema.getField("before").schema().getTypes().stream()
                .filter(s -> s.getType() == Schema.Type.RECORD).findFirst()
                .orElseThrow(() -> new RuntimeException("No record type found for 'before' field"));
        sourceSchema = envelopeSchema.getField("source").schema();

        GenericRecord beforeRecord = populateGenericRecord(before, beforeSchema);
        GenericRecord afterRecord = populateGenericRecord(after, afterSchema);
        GenericRecord sourceRecord = populateSourceRecord(sourceSchema);

        GenericRecord envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "source", sourceRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        return envelopeRecord;
    }

    private GenericRecord populateGenericRecord(String[] data, Schema schema) {
        if (data == null) {
            return null;
        }

        GenericRecord genericRecord = new GenericData.Record(schema);

        if (data.length != schema.getFields().size()) {
            throw new IllegalArgumentException(String.format(
                    "Invalid data size: expected %d fields but received %d.",
                    schema.getFields().size(), data.length
            ));
        }

        for (int i = 0; i < schema.getFields().size(); i++) {
            Schema.Field field = schema.getFields().get(i);
            Schema fieldSchema = SchemaFieldExtractor.unwrapNullableUnion(field.schema());
            Object value = parseValue(data[i], fieldSchema);

            try {
                // Ensure compatibility and handle type conversions where necessary
                value = toSchemaCompatibleValue(value, fieldSchema);

                genericRecord.put(field.name(), value);
            } catch (Exception e) {
                LOG.error("Field '{}' serialization failed. Expected type: {}, Value: '{}'.",
                        field.name(), fieldSchema.getType(), data[i], e);
                throw e;
            }
        }

        return genericRecord;
    }

    private GenericRecord populateSourceRecord(Schema sourceSchema) {
        GenericRecord sourceRecord = new GenericData.Record(sourceSchema);
        sourceRecord.put("version", "1.2.3"); // Example value
        sourceRecord.put("connector", "sqlserver"); // Example value
        sourceRecord.put("name", "sourceName"); // Example value
        sourceRecord.put("ts_ms", System.currentTimeMillis());
        sourceRecord.put("snapshot", null); // Allow null if permitted in schema
        sourceRecord.put("db", "test_db");
        sourceRecord.put("sequence", null); // Optional
        sourceRecord.put("ts_us", null); // Optional
        sourceRecord.put("ts_ns", null); // Optional
        sourceRecord.put("schema", "test_schema");
        sourceRecord.put("table", "test_table");
//        sourceRecord.put("change_lsn", null); // Optional
//        sourceRecord.put("commit_lsn", null); // Optional
//        sourceRecord.put("event_serial_no", null); // Optional
        return sourceRecord;
    }

    /**
     * Converts object to a schema-compatible value
     */
    private Object toSchemaCompatibleValue(Object value, Schema schema) {
        if (value == null) {
            return null; // Null is compatible met elk type
        }

        switch (schema.getType()) {
            case STRING:
                if (value instanceof byte[] bytes) {
                    return new String(bytes, StandardCharsets.UTF_8);
                }
                return value;

            case BYTES:
                if (value instanceof String string) {
                    return string.getBytes(StandardCharsets.UTF_8);
                }
                if (value instanceof byte[]) {
                    return value;
                }
                break;

            case UNION:
                for (Schema unionSchema : schema.getTypes()) {
                    if (unionSchema.getType() == Schema.Type.NULL) {
                        continue; // Skip NULL unions
                    }
                    try {
                        return toSchemaCompatibleValue(value, unionSchema);
                    } catch (Exception e) {
                        LOG.debug("Failed to convert value '{}' to schema type '{}' within union. Trying next type...",
                                value, unionSchema.getType(), e);
                    }
                }
                break;

            default:
                return value;
        }
        throw new IllegalArgumentException(String.format("Unsupported conversion for value '%s' with schema type '%s'", value, schema.getType()));
    }

    /**
     * Parses a String value into the appropriate type based on the field schema.
     */
    private Object parseValue(String value, Schema fieldSchema) {
        Schema baseSchema = SchemaFieldExtractor.unwrapNullableUnion(fieldSchema);

        switch (baseSchema.getType()) {
            case STRING:
                return value.equalsIgnoreCase("null") ? null : value;
            case INT:
                return value.equalsIgnoreCase("null") ? null : parseIntValue(value);
            case LONG:
                // Parse LONG: Handle "null" and scientific notation
                return value.equalsIgnoreCase("null") ? null : parseLongValue(value);
            case FLOAT:
                return value.equalsIgnoreCase("null") ? null : parseFloatValue(value);
            case DOUBLE:
                return value.equalsIgnoreCase("null") ? null : parseDoubleValue(value);
            case BOOLEAN:
                return value.equalsIgnoreCase("null") ? null : Boolean.parseBoolean(value);
            case ENUM:
                return new GenericData.EnumSymbol(baseSchema, value);
            case BYTES:
                return value.equalsIgnoreCase("null") ? null : value.getBytes(StandardCharsets.UTF_8);
            default:
                throw new IllegalArgumentException("Unsupported schema type: " + baseSchema.getType());
        }
    }

    /**
     * Parses a string value into a long, supporting scientific notation.
     */
    private Long parseLongValue(String value) {
        try {
            // First attempt to parse as a normal long
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            // Attempt to parse as a double and cast to a long
            return (long) Double.parseDouble(value);
        }
    }

    /**
     * Parses a string value into a double.
     */
    private Double parseDoubleValue(String value) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Cannot parse value '%s' as Double", value), e);
        }
    }

    /**
     * Parses a string value into an integer.
     */
    private Integer parseIntValue(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Cannot parse value '%s' as Integer", value), e);
        }
    }

    /**
     * Parses a string value into a float.
     */
    private Float parseFloatValue(String value) {
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(String.format("Cannot parse value '%s' as Float", value), e);
        }
    }

    private void setField(GenericRecord genericRecord, String fieldName, Object value) {

        Schema.Field field = genericRecord.getSchema().getField(fieldName);

        if (field != null) {
            Schema fieldSchema = field.schema();
            if (fieldSchema.getType() == Schema.Type.BYTES || SchemaFieldExtractor.unwrapNullableUnion(fieldSchema).getType() == Schema.Type.BYTES) {
                // Convert value to byte[] if the field is of type BYTES
                genericRecord.put(fieldName, convertToBytes(value));
            } else {
                genericRecord.put(fieldName, value);
            }
        } else {
            LOG.error("Field '{}' not found in schema: {}", fieldName, genericRecord.getSchema().getName());
            throw new IllegalArgumentException("Field " + fieldName + " is not valid for this schema.");
        }
    }

    private byte[] convertToBytes(Object value) {
        if (value instanceof byte[] bytes) {
            return bytes;
        }
        if (value instanceof String string) {
            // Convert base64-encoded or plain string into bytes
            return string.getBytes(StandardCharsets.UTF_8);
        }
        throw new IllegalArgumentException("Unsupported value for conversion to byte[]: " + value);
    }

    private static String[] datapart(String[] strings) {
        return Arrays.copyOfRange(strings, 3, strings.length);
    }

    public static void main(String[] args) {
        File file;

        // Create an instance of the dynamic Kafka producer
        DynamicTestKafkaProducer producerManager = new DynamicTestKafkaProducer();

        try {
            file = new File("C:\\Users\\HarrieVanRijn\\IdeaProjects\\Aicon-Tos-Mediator\\src\\main\\resources\\temp\\hvr.csv");
            List<String[]> csv = CsvParser.parseToList(file.getAbsolutePath(), true, String[].class);

            ConfigSettings.setConfigFile("C:\\Users\\HarrieVanRijn\\IdeaProjects\\Aicon-Tos-Mediator\\src\\main\\resources\\conf\\mediator\\DCT-DEV-aicon-connections.xml");

            // Dummy Avro-based messages (replace null with a valid Avro schema)
            String prefix = "tos.apex.dbo.";
            String topic1 = "inv_wi";
            String testTopic1 = prefix + "inv_wi_eli_tst";
            String testTopic2 = prefix + "inv_move_event_eli_tst";

            DynamicTestKafkaProducer dtp = new DynamicTestKafkaProducer();

            int i = 1;
            Boolean succes;

            succes = dtp.sendMessagToTestTopic(topic1, testTopic1, datapart(csv.get(i)), datapart(csv.get(i + 1)));
            if (!succes) {
                LOG.error("Failed to send message to topic: {}", topic1);
                return;
            }
            i++;
            succes = dtp.sendMessagToTestTopic(topic1, testTopic1, datapart(csv.get(i)), datapart(csv.get(i + 1)));
            if (!succes) {
                LOG.error("Failed to send message to topic: {}", topic1);
                return;
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            // Ensure all open producers are properly closed
            producerManager.closeAllProducers();
        }
    }

}
