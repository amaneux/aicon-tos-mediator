package com.aicon.tos.shared.kafka;

import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_value;

/**
 * A consumer class for Kafka messages related to the RESPONSE_TOPIC, topic that contains data for request.
 * This class listens to messages on the specified Kafka topic and processes
 * instances of {@link }.
 */
public class AiconTosConnectSwapResponseTopicConsumer extends
        KafkaConsumerBase<String, itv_job_resequence_response_value> {


    public AiconTosConnectSwapResponseTopicConsumer() {
        super(KafkaConfig.aicon_dispatch_itv_job_resequence_response_topic);

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        this.initializeConsumer(KafkaConfig.getConsumerProps());
    }
}
