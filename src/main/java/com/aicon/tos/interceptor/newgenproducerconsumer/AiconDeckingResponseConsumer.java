package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class AiconDeckingResponseConsumer extends KafkaConsumerBase<GenericRecord, GenericRecord> implements AutoCloseable {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingResponseConsumer.class);

    // Shared map to correlate requests and responses by unique keys
    private final ConcurrentHashMap<String, CompletableFuture<GenericRecord>> responseMap = new ConcurrentHashMap<>();
    private boolean isClosed = false; // Track whether this consumer is already closed
    private final boolean weCreateThisTopic = false;

    public AiconDeckingResponseConsumer(String differentGroupId, boolean weCreateThisTopic) {
        super(AiconDeckingConfig.getAiconDeckingResponseTopic(), differentGroupId, weCreateThisTopic);
    }

    public AiconDeckingResponseConsumer() {
        super(null, false);
    }

    @Override
    public void processRecords(ConsumerRecords<GenericRecord, GenericRecord> records) {
        if (records.count() != 0) {
            long timestamp = Instant.now().toEpochMilli();
            LOG.debug("Processing {} records for topic: {} at timestamp: {}", records.count(), topic, timestamp);
        }
        for (ConsumerRecord<GenericRecord, GenericRecord> record : records) {

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
}