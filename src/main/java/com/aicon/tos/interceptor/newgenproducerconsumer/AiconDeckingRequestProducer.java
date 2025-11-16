package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.aicon.tos.shared.kafka.KafkaProducerBase;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The AiconDeckingRequestProducer is responsible for producing Avro GenericRecord messages
 * to a specified Kafka topic for processing Aicon decking requests. It extends the
 * KafkaGenericProducerBase class, which provides common functionalities for Kafka producers.
 * <p>
 * This producer handles the creation of GenericRecord messages based on specific attributes
 * of an Aicon decking request and sends these messages to a specified Kafka topic.
 */
//public class AiconDeckingRequestProducer extends KafkaGenericProducerBase<GenericRecord, GenericRecord> {
public class AiconDeckingRequestProducer extends KafkaProducerBase<GenericRecord, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingRequestProducer.class);

    public AiconDeckingRequestProducer() {
        super(AiconDeckingConfig.getAiconDeckingRequestTopic(), false);
    }

    /**
     * Sends a given message (of type {@link GenericRecord}) to a designated Kafka topic.
     * Logs relevant information about the message being sent and any errors encountered during the sending process.
     * Additionally, provides feedback in the logs about the success of the operation, including the target topic.
     *
     * @param message the {@link GenericRecord} message to be sent to the Kafka topic
     */
    public void sendMessage(GenericRecord key, GenericRecord message) {
        LOG.info("Sending response message with key: {} to topic: {}", key, topic);
        LOG.info("Data: {}", message);

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
