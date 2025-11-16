package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconUserControlMessage;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer class for sending user control messages to the Kafka topic.
 * Extends {@link KafkaProducerBase} to provide functionality for sending
 * Avro-based messages to a specified Kafka topic.
 */
public class AiconUserControlProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconUserControlProducer.class);

    /**
     * Constructs an {@code AiconUserControlProducer} and initializes it with
     * the topic {@code AICON_USER_CONTROL_TOPIC}.
     */
    public AiconUserControlProducer() {
        super(KafkaConfig.AICON_USER_CONTROL_TOPIC, true);
        ProducerManager.registerProducer(this);
    }

    /**
     * Creates and sends a user control message to the Kafka topic.
     *
     * @param id                The unique identifier for the message.
     * @param userOperatingMode The operating mode of the user, provided as a string.
     * @throws IllegalArgumentException If the {@code userOperatingMode} is invalid.
     */
    public void sendAiconUserControlMessage(String id, String userOperatingMode, Boolean ignore) {
        AiconUserControlMessage message = new AiconUserControlMessage();
        try {
            message.setId(id);
            message.setUserOperatingMode(UserOperatingMode.valueOf(userOperatingMode));
            message.setIgnore(ignore);
        } catch (Exception e) {
            LOG.error("Field does not exist in schema: {}", e.getMessage(), e);
            throw e;
        }
        sendMessage(id, message);
    }
}
