package com.aicon.tos.shared.kafka;


import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Properties;

public class AiconYardDeckingUpdateRequestConsumer extends KafkaConsumerBase<String, AiconYardDeckingUpdateRequestMessage> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconYardDeckingUpdateRequestConsumer.class);

    public AiconYardDeckingUpdateRequestConsumer(String differentGroupId) {
        super(KafkaConfig.getFlowRequestTopic("DeckingUpdateFlow"));

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        Properties props = KafkaConfig.getConsumerProps(differentGroupId);
        props.put(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG, true);
        this.initializeConsumer(props);
    }

    public AiconYardDeckingUpdateRequestConsumer() {
        this(null);
    }

    /**
     * Starts consuming messages from the Kafka topic in an infinite loop.
     * Processes each record and notifies all registered listeners of the received messages.
     */
    public void consumeMessages() {
        Thread.currentThread().setName("TosDeckingUpdateForm");
        pollingWorkerThread.set(Thread.currentThread()); // Atomically set the current thread
        try {
            while (running) {
                try {
                    ConsumerRecords<String, AiconYardDeckingUpdateRequestMessage> records =
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
    public void processRecords(ConsumerRecords<String, AiconYardDeckingUpdateRequestMessage> records) {
        // not sed yet, likely for mockups
    }
}
