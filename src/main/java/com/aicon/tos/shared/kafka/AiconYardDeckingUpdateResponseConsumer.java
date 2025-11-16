package com.aicon.tos.shared.kafka;


import com.aicon.tos.shared.schema.AiconYardDeckingUpdateResponseMessage;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificData;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AiconYardDeckingUpdateResponseConsumer extends KafkaConsumerBase<GenericRecord, GenericRecord> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AiconYardDeckingUpdateResponseConsumer.class);

    // Shared map to correlate requests and responses by unique keys
    private final ConcurrentHashMap<String, CompletableFuture<GenericRecord>> responseMap = new ConcurrentHashMap<>();
    private final List<AiconYardDeckingUpdateMessageListener> listeners = new ArrayList<>();
    private boolean isClosed = false; // Track whether this consumer is already closed

    public AiconYardDeckingUpdateResponseConsumer(String differentGroupId) {
        super(KafkaConfig.getFlowResponseTopic("DeckingUpdateFlow"), differentGroupId, true);
    }

    public AiconYardDeckingUpdateResponseConsumer() {
        this(null);
    }


    public void handleMessage(ConsumerRecord<GenericRecord, GenericRecord> record) {
        GenericRecord key = record.key();
        GenericRecord value = record.value();
        String keyvalue = key.get("request_index").toString();

        // Check if a request is waiting for this key
        if (responseMap.containsKey(keyvalue)) {
            LOG.info("Received matching response for key: {}", key);
            // Complete the future for the given key
            CompletableFuture<GenericRecord> future = responseMap.remove(keyvalue); // Remove the key after completing
            if (future != null) {
                future.complete(value); // Provide the response to the waiting thread
            }
        } else {
            LOG.warn("Received response with unknown or unregistered key: {}", key);
            // Optionally handle "orphan" messages that do not match any key
        }
    }


    /**
     * Remove a request mapping in case of timeout or cancellation.
     */
    public void removeRequest(String key) {
        responseMap.remove(key);
    }

    @Override
    public void close() {
        if (!isClosed) {
            try {
                // Perform resource cleanup here (e.g., Kafka consumer cleanup)
                LOG.info("Closing AiconDeckingResponseConsumer...");

                // Mark this resource as closed to avoid duplicate cleanup
                isClosed = true;

                LOG.info("AiconDeckingResponseConsumer closed successfully.");
            } catch (Exception e) {
                // Log any issue during the cleanup process
                LOG.error("Error while closing AiconDeckingResponseConsumer: {}", e.getMessage(), e);
            }
        }
    }

    //    /**
//     * Starts consuming messages from the Kafka topic in an infinite loop.
//     * Processes each record and notifies all registered listeners of the received messages.
//     */
    public void consumeResponseMessages() {
//        Thread.currentThread().setName("TosDeckingUpdateForm");
//        pollingWorkerThread.set(Thread.currentThread()); // Atomically set the current thread
//        try {
//            while (running) {
//                try {
        ConsumerRecords<GenericRecord, GenericRecord> records = this.poll(Duration.ofMillis(KafkaConfig.POLL_WAIT_IN_MSEC));

        if (records.count() > 0) {
            processRecords(records);
        }
    }


    //
//
//    /**
//     * Processes the consumed records from the Kafka topic. Converts each message to its operating mode
//     * and notifies all registered listeners.
//     *
//     * @param records The {@link ConsumerRecords} to process.
//     */
    public void processRecords(ConsumerRecords<GenericRecord, GenericRecord> records) {
        records.forEach(singleRecord -> {
            GenericRecord genericRecord = singleRecord.value();
            AiconYardDeckingUpdateResponseMessage message = (AiconYardDeckingUpdateResponseMessage)
                    SpecificData.get().deepCopy(AiconYardDeckingUpdateResponseMessage.SCHEMA$, genericRecord);

            if (message == null || message.getMoves() == null) {
                LOG.warn("Received null or empty AiconYardDeckingUpdateResponseMessage.");
                return;
            }

            LOG.info("Send DeckingUpdateResponseMessage to listeners (record count: {}).", records.count());

            message.getMoves().forEach(move -> {
                if (move == null) {
                    LOG.debug("DeckMove is null for a move, skipping.");
                    return;
                }
                LOG.debug("COMMENTED!!! Notifying listener(s) with resultCode: {}", move.getResult().getResultCode());
                //TODO: Notify listeners with the move's resultCode and resultText.'
//              listeners.forEach(listener -> listener.onMessageReceived(resultCode, resultText, resultAudience, resultLevel));
            });
        });
    }

    /**
     * Adds a {@link AiconTosControlMessageListener} to the consumer. The listener will be notified
     * whenever a new message is consumed from the Kafka topic.
     *
     * @param listener The listener to be added.
     */
    public void addMessageListener(AiconYardDeckingUpdateMessageListener listener) {
        LOG.info("Message listener added");
        listeners.add(listener);
    }

}
