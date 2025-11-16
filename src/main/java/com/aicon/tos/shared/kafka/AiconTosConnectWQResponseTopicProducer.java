package com.aicon.tos.shared.kafka;

import com.avlino.aicon.WorkQueueActivationResponse.dispatch_work_queue_activation_response_value;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for producing and sending Kafka messages related to the topic.
 * It constructs messages based on the provided parameters and uses the Avro schema defined in {@link }.
 */
public class AiconTosConnectWQResponseTopicProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectWQResponseTopicProducer.class);

    public AiconTosConnectWQResponseTopicProducer() {
        super(KafkaConfig.aicon_dispatch_work_queue_activation_response_topic, false);
        ProducerManager.registerProducer(this);
    }

    public void sendSwapResponseTopicMessage(String request_id) {

        dispatch_work_queue_activation_response_value message = new dispatch_work_queue_activation_response_value();
        try {
            message.setRequestIdx(request_id);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid response for request_id: {}", request_id);
            throw e;
        }

        sendMessage(request_id, message);
    }
}