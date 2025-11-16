package com.aicon.tos.shared.kafka;

/**
 * Interface for listening to messages from a Kafka topic.
 * Implementations of this interface should define the behavior
 * when a message containing the operating mode is received.
 */
public interface AiconYardDeckingUpdateMessageListener {

    void onMessageReceived(String code, String resultText, String resultCode, String resultComment);
}