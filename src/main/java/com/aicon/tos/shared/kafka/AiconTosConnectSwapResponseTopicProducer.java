package com.aicon.tos.shared.kafka;

import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_value;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for producing and sending Kafka messages related to the AICON TOS Connection Status.
 * It constructs messages based on the provided parameters and uses the Avro schema defined in {@link }.
 */
public class AiconTosConnectSwapResponseTopicProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectSwapResponseTopicProducer.class);

    /**
     * Constructor that initializes the producer with the AICON TOS Connection Status topic.
     */
    public AiconTosConnectSwapResponseTopicProducer() {
        super(KafkaConfig.aicon_dispatch_itv_job_resequence_response_topic, false);
        ProducerManager.registerProducer(this);
    }

    /**
     * Sends a connection status message to the AICON TOS Connection Status Kafka topic.
     *
     * @param request_id The unique request ID for the message.
     */
    public void sendSwapResponseTopicMessage(String request_id) {

        itv_job_resequence_response_value message = new itv_job_resequence_response_value();
        try {
            message.setRequestIdx(request_id);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid response for request_id: {}", request_id);
            throw e;
        }

        sendMessage(request_id, message);
    }
}