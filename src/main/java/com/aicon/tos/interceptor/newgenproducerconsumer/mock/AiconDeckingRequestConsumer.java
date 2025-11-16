package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingConfig;
import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Consumer implementation for processing Kafka messages from a specific topic
 * related to Aicon decking requests. This class extends the {@code KafkaGenericConsumerBase}
 * class, providing a specific behavior for handling messages received in the topic.
 * <p>
 * This consumer is configured to process messages where the key is of type {@code String}
 * and the value is represented by {@code GenericRecord}. The {@code handleMessage} method
 * is overridden to define the specific processing logic for the consumed messages.
 */
public class AiconDeckingRequestConsumer extends KafkaConsumerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingRequestConsumer.class);

    public AiconDeckingRequestConsumer() {
        super(AiconDeckingConfig.getAiconDeckingRequestTopic(), false);
    }

    /**
     * Handles the processing of the received Kafka message.
     * This method is invoked to process a single {@code ConsumerRecord}
     * containing a key of type {@code String} and a value of type {@code GenericRecord}.
     * Typically, the message can be logged, forwarded to additional handlers, or passed to
     * downstream processing components for further actions.
     *
     * @param records the {@code ConsumerRecord} representing the Kafka message to be processed,
     *                containing the message key and value
     */
    @Override
    public void processRecords(ConsumerRecords<String, GenericRecord> records) {
        for (ConsumerRecord<String, GenericRecord> record : records) {
            LOG.info("Received key: {}, value: {}", record.key(), record.value());
            // you could forward to a handler class or pass to downstream processing here
        }
    }
}
