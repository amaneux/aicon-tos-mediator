package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconTosControlRuleTableMessage;
import com.aicon.tos.shared.schema.OperatingModeRule;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Producer class for sending TOS (Terminal Operating System) control messages to the Kafka topic.
 * Extends {@link KafkaProducerBase} to provide functionality for sending Avro-based messages
 * of type {@link AiconTosControlRuleTableMessage} to a specified Kafka topic.
 */
public class AiconTosControlRuleTableProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlRuleTableProducer.class);

    /**
     * Constructs an {@code AiconTosControlProducer} and initializes it with
     * the topic {@code AICON_TOS_CONTROL_RULE_TABLE_TOPIC}.
     */
    public AiconTosControlRuleTableProducer() {
        super(KafkaConfig.AICON_TOS_CONTROL_RULE_TABLE_TOPIC, true);
        ProducerManager.registerProducer(this);
    }

    public void sendRuleTableMessage(List<OperatingModeRule> rules) {
        AiconTosControlRuleTableMessage message = new AiconTosControlRuleTableMessage();
        try {
            message.setRules(rules);
        } catch (Exception e) {
            LOG.error("Field does not exist in schema: {}", e.getMessage(), e);
            throw e;
        }
        sendMessage( "1", message);
    }
}
