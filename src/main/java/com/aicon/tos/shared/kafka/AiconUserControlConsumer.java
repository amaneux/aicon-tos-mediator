package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconUserControlMessage;

/**
 * Consumer class for receiving user control messages from the Kafka topic.
 * Extends {@link KafkaConsumerBase} to provide functionality for consuming
 * {@link AiconUserControlMessage} messages from a specified Kafka topic.
 */
public class AiconUserControlConsumer extends KafkaConsumerBase<String, AiconUserControlMessage> {

    /**
     * Constructs an {@code AiconUserControlConsumer} and subscribes it to the
     * {@code AICON_USER_CONTROL_TOPIC} Kafka topic.
     * Initializes the consumer with the default Kafka consumer properties.
     */
    public AiconUserControlConsumer() {
        super(KafkaConfig.AICON_USER_CONTROL_TOPIC);
        this.initializeConsumer(KafkaConfig.getConsumerProps());
    }
}
