package com.aicon.tos.shared.kafka;


import com.aicon.tos.shared.schema.AiconTosControlMessage;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Consumer class for receiving TOS (Terminal Operating System) control messages from the Kafka topic.
 * Extends {@link KafkaConsumerBase} to provide functionality for consuming
 * {@link AiconTosControlMessage} messages and notifying registered listeners.
 */
public class AiconTosControlConsumer extends KafkaConsumerBase<String, AiconTosControlMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlConsumer.class);

    private final List<AiconTosControlMessageListener> listeners = new ArrayList<>();

    public AiconTosControlConsumer(String differentGroupId) {
        super(KafkaConfig.AICON_TOS_CONTROL_TOPIC);

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        this.initializeConsumer(KafkaConfig.getConsumerProps(differentGroupId));
    }

    public AiconTosControlConsumer() {
        this(null);
    }


    /**
     * Adds a {@link AiconTosControlMessageListener} to the consumer. The listener will be notified
     * whenever a new message is consumed from the Kafka topic.
     *
     * @param listener The listener to be added.
     */
    public void addMessageListener(AiconTosControlMessageListener listener) {
        LOG.info("Message listener added");
        listeners.add(listener);
    }

    /**
     * Starts consuming messages from the Kafka topic in an infinite loop.
     * Processes each record and notifies all registered listeners of the received messages.
     */
    public void consumeMessages() {
        Thread.currentThread().setName("AiconTosControlForm");
        pollingWorkerThread.set(Thread.currentThread()); // Atomically set the current thread
        try {
            while (running) {
                try {
                    ConsumerRecords<String, AiconTosControlMessage> records =
                            poll(Duration.ofMillis(KafkaConfig.POLL_WAIT_IN_MSEC));
                    if (running && records.count() > 0) {
                        processRecords(records);
                    }
                } catch (WakeupException e) {
                    if (!running) {
                        LOG.info("Polling interrupted for topic: {}, consumer is shutting down.", topic);
                    } else {
                        LOG.error("Unexpected wakeup exception during polling for topic: {}", topic, e);
                    }
                }
            }
        } finally {
            LOG.info("Polling thread exiting for topic: {}", topic);
        }
    }


    /**
     * Processes the consumed records from the Kafka topic. Converts each message to its operating mode
     * and notifies all registered listeners.
     *
     * @param records The {@link ConsumerRecords} to process.
     */
    public void processRecords(ConsumerRecords<String, AiconTosControlMessage> records) {
        records.forEach(record -> {
            String operatingMode = record.value().getOperatingMode().toString();
            Boolean ignore = record.value().getIgnore();
            long timeSync = record.value().getTimeSync();
            String timeSyncStr = timeSync + " msec";
            if (timeSync > 0L) {
                timeSyncStr = timeSyncStr + "  (+ = AICON behind TOS)";
            } else {
                timeSyncStr = timeSyncStr + " (- = AICON ahead of TOS)";
            }
            String comment = record.value().getComment().toString();

            LOG.info("Send message to listeners");
            // Notify all registered listeners that a message has been received
            String finalTimeSyncStr = timeSyncStr;
            listeners.forEach(listener -> listener.onMessageReceived(operatingMode, finalTimeSyncStr, comment, ignore));

            LOG.info("Consumed record with key: {} and value: {}", record.key(), operatingMode);
        });
    }
}
