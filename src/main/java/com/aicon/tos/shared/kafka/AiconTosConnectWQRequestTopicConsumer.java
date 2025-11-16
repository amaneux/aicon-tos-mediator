package com.aicon.tos.shared.kafka;

import com.avlino.aicon.WorkQueueActivationRequest.dispatch_work_queue_activation_value;

/**
 * A consumer class for Kafka messages related to the REQUEST_TOPIC, topic that contains data for request.
 * This class listens to messages on the specified Kafka topic and processes
 * instances of {@link }.
 */
public class AiconTosConnectWQRequestTopicConsumer extends
        KafkaConsumerBase<String, dispatch_work_queue_activation_value> {

    public AiconTosConnectWQRequestTopicConsumer() {
        super(KafkaConfig.aicon_dispatch_work_queue_activation_request_topic);

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        this.initializeConsumer(KafkaConfig.getConsumerProps());
    }
}
