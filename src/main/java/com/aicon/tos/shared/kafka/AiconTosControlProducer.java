package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconTosControlMessage;
import com.aicon.tos.shared.schema.OperatingMode;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Producer class for sending TOS (Terminal Operating System) control messages to the Kafka topic.
 * Extends {@link KafkaProducerBase} to provide functionality for sending Avro-based messages
 * of type {@link AiconTosControlMessage} to a specified Kafka topic.
 */
public class AiconTosControlProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlProducer.class);

    /**
     * Constructs an {@code AiconTosControlProducer} and initializes it with
     * the topic {@code AICON_TOS_CONTROL_TOPIC}.
     */
    public AiconTosControlProducer() {
        super(KafkaConfig.AICON_TOS_CONTROL_TOPIC, true);
        ProducerManager.registerProducer(this);
    }

    /**
     * Creates and sends a TOS control message to the Kafka topic.
     *
     * @param id            The unique identifier for the message.
     * @param operatingMode The operating mode as a string, which will be converted to {@link OperatingMode}.
     * @param ignore     Indicates to the receiver that the system is not really sending controls
     * @param timeSync      The time sync value, temporary solution to get the value in the mockup
     * @param comment       Claryfing the outcome
     * @throws IllegalArgumentException If the {@code operatingMode} is invalid or does not match any value in {@link OperatingMode}
     */
    public void sendAiconTosControlMessage(String id, String operatingMode, Boolean ignore, Long timeSync, String comment) {
        AiconTosControlMessage message = new AiconTosControlMessage();
        try {
            message.setId(id);
            message.setOperatingMode(OperatingMode.valueOf(operatingMode));
            message.setIgnore(ignore);
            message.setTimeSync(timeSync);
            message.setComment(comment);
        } catch (Exception e) {
            LOG.error("Field does not exist in schema: {}", e.getMessage(), e);
            throw e;
        }
        sendMessage( id, message);
    }
}
