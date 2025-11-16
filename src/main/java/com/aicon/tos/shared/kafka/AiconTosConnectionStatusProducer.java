package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;
import com.aicon.tos.shared.schema.ConnectionStatus;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is responsible for producing and sending Kafka messages related to the AICON TOS Connection Status.
 * It constructs messages based on the provided parameters and uses the Avro schema defined in {@link AiconTosConnectionStatusMessage}.
 */
public class AiconTosConnectionStatusProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectionStatusProducer.class);

    /**
     * Constructor that initializes the producer with the AICON TOS Connection Status topic.
     */
    public AiconTosConnectionStatusProducer() {
        super(KafkaConfig.AICON_TOS_CONNECTION_STATUS_TOPIC, true);
        ProducerManager.registerProducer(this);
    }

    /**
     * Sends a connection status message to the AICON TOS Connection Status Kafka topic.
     *
     * @param requestId          The unique request ID for the message.
     * @param connectionStatusStr The status of the connection as a string.
     * @param cdcOk               The CDC connection is ok.
     */
    public void sendConnectionStatusMessage(String requestId,
                                            String connectionStatusStr,
                                            Boolean cdcOk) {

        AiconTosConnectionStatusMessage message = new AiconTosConnectionStatusMessage();
        try {
            message.setRequestId(requestId);
            message.setConnectionStatus(ConnectionStatus.valueOf(connectionStatusStr));
            message.setCdcOk(cdcOk);
        } catch (IllegalArgumentException e) {
            LOG.error("Invalid connection status: {} for request_id: {}", connectionStatusStr, requestId);
            throw e;
        }

        sendMessage (requestId, message);
    }

}