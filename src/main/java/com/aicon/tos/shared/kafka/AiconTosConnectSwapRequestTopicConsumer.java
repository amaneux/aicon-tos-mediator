package com.aicon.tos.shared.kafka;

import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_value;

/**
 * A consumer class for Kafka messages related to the REQUEST_TOPIC, topic that contains data for request.
 * This class listens to messages on the specified Kafka topic and processes
 * instances of {@link }.
 */
public class AiconTosConnectSwapRequestTopicConsumer extends
        KafkaConsumerBase<String, itv_job_resequence_request_value> {

    public AiconTosConnectSwapRequestTopicConsumer() {
        super(KafkaConfig.aicon_dispatch_itv_job_resequence_request_topic);

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        this.initializeConsumer(KafkaConfig.getConsumerProps());
    }
}
