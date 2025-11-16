package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.interceptor.newgenproducerconsumer.SchemaLoader;
import com.aicon.tos.interceptor.newgenproducerconsumer.mock.AiconDeckingResponseProducer;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * In short creates a temporary topic and schema based on the aicon_decking_engine_respons_v2
 * Assums no temporary topic and schema exists
 * <p>
 * The {@code AiconDeckingResponseProducerMain} class serves as the main entry point for producing
 * and sending Avro-based messages to a new Kafka topic based on a original.
 * original = aicon_decking_engine_response_v2
 * new = as defined in the config
 * <p>
 * The new topic is als used by the AiconDeckingEngineMockup to mock up the real AiconDeckingEngine
 * <p>
 * It interacts with the {@code AiconDeckingResponseProducer}
 * to send messages containing container and decking information.
 * <p>
 * This class handles schema retrieval from a registry, creates a message with specific container
 * and decking request details, and sends it to the Kafka topic defined by the producer.
 * It also includes error handling for schema loading and message delivery to ensure proper execution.
 * <p>
 * Responsibilities:
 * - Retrieve the Avro schema required for the message payload from a schema registry.
 * - Create generic Avro records using the schema.
 * - Populate and structure the message with container and decking information.
 * - Send the message to the Kafka topic handled by the producer.
 */


public class AiconDeckingResponseProducerMain {
    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingResponseProducerMain.class);

    private static final AiconDeckingResponseProducer producer = new AiconDeckingResponseProducer();
    private static final String originalTopic = "aicon_decking_request_v2";
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
        request.put("containerNumber", "CONT9876543");
        request.put("containerVisitId", "VISIT321");
        request.put("jobType", "UNLOAD");
        request.put("uptUserId", "USER99");
        request.put("programId", "PGM11");
        request.put("blockIndexNumber", "B5");
        request.put("bayIndexNumber", "03");
        request.put("rowIndexNumber", "07");
        request.put("tierIndexNumber", "02");
        request.put("internalIndex1", null);
        request.put("internalIndex2", null);
        request.put("requestedBlockOnly", false);
        request.put("containerISOCode", "22U1");
        request.put("containerISOGroup", "VH");
        request.put("isEmptyContainer", false);
        request.put("errorCode", null);
        request.put("errorDesc", null);

        GenericRecord message = new GenericData.Record(originalSchema);
        message.put("requestIndex", "REQ-20250408-RESP-001");
        message.put("count", 1);
        message.put("timeStamp", System.currentTimeMillis());
        message.put("errorCode", null);
        message.put("errorDesc", null);
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
