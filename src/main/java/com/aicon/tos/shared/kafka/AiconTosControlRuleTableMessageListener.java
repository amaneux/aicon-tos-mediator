package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.OperatingModeRule;

import java.util.List;

/**
 * Interface for listening to messages from a Kafka topic.
 * Implementations of this interface should define the behavior
 * when a message containing the operating mode is received.
 */
public interface AiconTosControlRuleTableMessageListener {

    /**
     * Callback method to handle a received message.
     *
     * @param operatingMode The operating mode received as a String.
     * @param timeSync      The time synchronization received as a String.
     */
    void onMessageReceived(List<OperatingModeRule> ruleTable);
}