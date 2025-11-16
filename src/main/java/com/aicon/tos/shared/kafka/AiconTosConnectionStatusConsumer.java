package com.aicon.tos.shared.kafka;


import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;

/**
 * A consumer class for Kafka messages related to the AICON TOS Connection Status.
 * This class listens to messages on the specified Kafka topic and processes
 * instances of {@link AiconTosConnectionStatusMessage}.
 */
public class AiconTosConnectionStatusConsumer extends KafkaConsumerBase<String, AiconTosConnectionStatusMessage> {

    /**
     * Constructor that initializes the consumer with the AICON TOS Connection Status topic.
     * It also sets up the consumer with the necessary Kafka properties.
     */
    public AiconTosConnectionStatusConsumer() {
        super(KafkaConfig.AICON_TOS_CONNECTION_STATUS_TOPIC);

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        this.initializeConsumer(KafkaConfig.getConsumerProps());
    }
}
