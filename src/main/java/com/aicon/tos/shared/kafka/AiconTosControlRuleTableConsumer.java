package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconTosControlRuleTableMessage;
import com.aicon.tos.shared.schema.OperatingModeRule;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Consumer class for receiving TOS (Terminal Operating System) control messages from the Kafka topic.
 * Extends {@link KafkaConsumerBase} to provide functionality for consuming
 * {@link AiconTosControlRuleTableMessage} messages and notifying registered listeners.
 */
public class AiconTosControlRuleTableConsumer extends KafkaConsumerBase<String, AiconTosControlRuleTableMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlRuleTableConsumer.class);

    private final List<AiconTosControlRuleTableMessageListener> listeners = new ArrayList<>();

    /**
     * Constructs an {@code AiconTosControlConsumer} and subscribes it to the
     * {@code AICON_TOS_CONTROL_RULE_TABLE_TOPIC} Kafka topic.
     */
    public AiconTosControlRuleTableConsumer() {
        super(KafkaConfig.AICON_TOS_CONTROL_RULE_TABLE_TOPIC);
    }

    /**
     * Adds a {@link AiconTosControlMessageListener} to the consumer. The listener will be notified
     * whenever a new message is consumed from the Kafka topic.
     *
     * @param listener The listener to be added.
     */
    public void addMessageListener(AiconTosControlRuleTableMessageListener listener) {
        LOG.info("Message listener added");
        listeners.add(listener);
    }

    /**
     * Starts consuming messages from the Kafka topic in an infinite loop.
     * Processes each record and notifies all registered listeners of the received messages.
     */
    public void consumeMessages() {
        try {
            while (this.running) {
                ConsumerRecords<String, AiconTosControlRuleTableMessage> records = pollMessages();
                if (records.count() > 0) {
                    processRecords(records);
                }
            }
        } catch (WakeupException e) {
            // Ignore exception if closing
            if (running) throw e;
        } catch (Exception e) {
            LOG.error("Error while consuming messages: ", e);
        } finally {
            close();
        }
    }

    /**
     * Starts consuming messages from the Kafka topic in an infinite loop.
     * Processes each record and notifies all registered listeners of the received messages.
     */
    public List<OperatingModeRule> readMessage() {
        try {
            ConsumerRecords<String, AiconTosControlRuleTableMessage> records = pollMessages();
            final List<OperatingModeRule>[] ruleTable = new List[]{new ArrayList<>()};
            if (records.count() != 1) {
                LOG.error("Expected 1 record");
            } else {
                records.forEach(record -> {
                    ruleTable[0] = record.value().getRules();
                });
                return ruleTable[0];
            }
        } catch (
                Exception e) {
            LOG.error("Error while consuming messages: ", e);
        } finally {
            close();
        }
        return null;
    }

    /**
     * Processes the consumed records from the Kafka topic. Converts each message to its operating mode
     * and notifies all registered listeners.
     *
     * @param records The {@link ConsumerRecords} to process.
     */
    public void processRecords(ConsumerRecords<String, AiconTosControlRuleTableMessage> records) {
        records.forEach(record -> {
            List<OperatingModeRule> rules = record.value().getRules();

            LOG.info("Send message to listeners");
            // Notify all registered listeners that a message has been received
            listeners.forEach(listener -> listener.onMessageReceived(rules));

            LOG.info("Consumed record with key: {} and value: {}", record.key(), rules);
        });
    }
}
