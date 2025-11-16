package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingConfig;
import com.aicon.tos.interceptor.newgenproducerconsumer.GenericAvroBuilder;
import com.aicon.tos.interceptor.newgenproducerconsumer.KafkaGenericProducerBase;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The AiconDeckingResponseProducer class is responsible for producing and sending
 * Kafka messages to a specified topic. It extends KafkaGenericProducerBase and specifically
 * deals with messages using a String key and a GenericRecord value.
 * <p>
 * This producer generates Avro GenericRecords for specific business logic, logs its activity,
 * and provides lifecycle management like closing the Kafka producer.
 */
public class AiconDeckingResponseProducer extends KafkaGenericProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingResponseProducer.class);
    private final String topic;
    private String key = "";

    public AiconDeckingResponseProducer() {
        super(AiconDeckingConfig.getAiconDeckingResponseTopic(), false);
        this.topic = getTopicName();
    }

    public GenericRecord createMessage(String key, List<Map<String, Object>> requestItems, Integer errorCode, String errorDesc) {
        this.key = key;

        List<Map<String, Object>> requests = new java.util.ArrayList<>();

        for (Map<String, Object> requestItem : requestItems) {
            Map<String, Object> request = new HashMap<>();
            request.putAll(requestItem);
            requests.add(request);
        }

        Map<String, Object> input = new HashMap<>();
        input.put("requestIndex", key);
        input.put("count", requests.size());
        input.put("timeStamp", System.currentTimeMillis());
        input.put("errorCode", errorCode);
        input.put("errorDesc", errorDesc);
        input.put("requests", requests);

        return GenericAvroBuilder.buildMessage(topic, input);
    }

    /**
     * Sends a given message (of type {@link GenericRecord}) to a designated Kafka topic.
     * Logs relevant information about the message being sent and any errors encountered during the sending process.
     * Additionally, provides feedback in the logs about the success of the operation, including the target topic.
     *
     * @param message the {@link GenericRecord} message to be sent to the Kafka topic
     */
    public void sendMessage(GenericRecord message) {
        LOG.info("Sending response message with key: {} to topic: {}", key, topic);
        LOG.info("Data: {}", message.toString());
        sendMessage(key, message, (metadata, ex) -> {
            if (ex != null) {
                LOG.error("Send failed: {}", ex.getMessage(), ex);
            } else {
                LOG.info("Message sent to {}", metadata.topic());
            }
        });
    }

    /**
     * Close the producer and release all resources.
     */
    @Override
    public void close() {
        if (producer != null) {
            try {
                producer.close();
                LOG.info("Kafka producer for topic: {} closed.", topic);
            } catch (Exception e) {
                LOG.error("Error while closing Kafka producer for topic: {}", topic, e);
            }
        }
    }
}
