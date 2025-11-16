package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingRequestProducer;
import com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * In short creates a temporary topic and schema based on the aicon_decking_engine_request_v2
 * Assums no temporary topic and schema exists
 *
 * The {@code AiconDeckingRequestProducerMain} class serves as the main entry point for producing
 * and sending Avro-based messages to a new Kafka topic based on a original.
 * original = aicon_decking_engine_request_v2
 * new = as defined in the config
 *
 * The new topic is als used by the AiconDeckingEngineMockup to mock up the real AiconDeckingEngine
 *
 * It interacts with the {@code AiconDeckingRequestProducer}
 * to send messages containing container and decking information.
 *
 * This class handles schema retrieval from a registry, creates a message with specific container
 * and decking request details, and sends it to the Kafka topic defined by the producer.
 * It also includes error handling for schema loading and message delivery to ensure proper execution.
 *
 * Responsibilities:
 * - Retrieve the Avro schema required for the message payload from a schema registry.
 * - Create generic Avro records using the schema.
 * - Populate and structure the message with container and decking information.
 * - Send the message to the Kafka topic handled by the producer.
 */


public class AiconDeckingRequestProducerMain {
    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingRequestProducerMain.class);

    private static AiconDeckingRequestProducer producer = new AiconDeckingRequestProducer();
    private static String originalTopic =  "aicon_decking_request_v2";
    private static final Schema keySchema = SchemaLoader.getKeySchemaFromRegistry(originalTopic);
    private static final Schema originalSchema = SchemaLoader.getValueSchemaFromRegistry(originalTopic);

    public static void main(String[] args) {

        Schema schema = null;
        try {
            schema = SchemaLoader.getAllFromRegistry("hvr");
        } catch (Exception e) {
            System.exit(1);
        }
        if (schema == null) {
            createRegistryEntry();
        }
    }

    public static void createRegistryEntry() {
        String uniqueKey = "21021961T";
        GenericRecord keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("request_index", uniqueKey);

        // Create message
        GenericRecord request = new GenericData.Record(originalSchema.getField("requests").schema().getElementType());
        request.put("containerNumber", "CONT123");
        request.put("containerVisitId", "VIS123");
        request.put("jobType", "LOAD");
        request.put("uptUserId", "USR1");
        request.put("programId", "PGM1");
        request.put("blockIndexNumber", "B1");
        request.put("bayIndexNumber", "05");
        request.put("rowIndexNumber", "03");
        request.put("tierIndexNumber", "01");
        request.put("internalIndex1", null);
        request.put("internalIndex2", null);
        request.put("requestedBlockOnly", false);
        request.put("isEmptyContainer", false);
        request.put("containerNumber", "UNKNOWN");
        request.put("containerVisitId", "-1");

        GenericRecord message = new GenericData.Record(originalSchema);
        message.put("requestIndex", "REQ-001");
        message.put("count", 1);
        message.put("timeStamp", System.currentTimeMillis());
        message.put("requests", List.of(request));

        // Send it
        ProducerRecord<GenericRecord, GenericRecord> producerRecord = new ProducerRecord<>(producer.getTopicName(), keyRecord, message);
        producer.sendMessage(producerRecord, (metadata, exception) -> {
            if (exception != null) {
                exception.printStackTrace();
            } else {
                LOG.info("Sent to topic: {}", metadata.topic());
            }
        });

        producer.close();
    }
}
