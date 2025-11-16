package com.aicon.tos.shared.kafka;

import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;

/**
 * The BufferedKafkaConsumer is an implementation of a Kafka consumer that buffers the consumed
 * messages in a thread-safe queue for later processing. It adds the capability of handling the
 * deduplication and removal of older records from the buffer, allowing consumers to retrieve
 * and interact with buffered messages efficiently.
 * <p>
 * This class extends KafkaConsumerBase, inheriting core Kafka consumer functionalities
 * like initializing, polling, and stopping the consumer.
 */
public class BufferedKafkaConsumer extends KafkaConsumerBase<Object, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(BufferedKafkaConsumer.class);
    private final BlockingQueue<MessageMetadata> buffer = new LinkedBlockingQueue<>();
    private final AtomicBoolean polling = new AtomicBoolean(false);

    public BufferedKafkaConsumer(String topic) {
        super(topic);
    }

    /**
     * Initiates the polling process to continuously consume messages from the associated Kafka topic.
     * This method spawns a dedicated polling thread that fetches messages from the configured Kafka topic
     * at regular intervals and processes them. The polling thread operates as long as the `running` flag
     * of the consumer remains true, or until it encounters an unrecoverable exception.
     * <p>
     * If a polling thread is already active for the Kafka topic, a warning will be logged, and no new
     * thread will be created.
     * <p>
     * The polling operations include:
     * - Fetching messages using the `pollMessages` method.
     * - Processing fetched messages through the `processRecords` method.
     * - Continuing to poll and sleep in a loop until interrupted.
     * <p>
     * Thread interruption during sleep is handled, allowing the thread to gracefully log the interruption
     * and continue working or terminate depending on the `running` flag.
     * <p>
     * This method ensures thread-safety by using `compareAndSet` on the `polling` flag to coordinate
     * the state of the polling process. Only one polling thread can be active for the Kafka consumer
     * at any given time.
     * <p>
     * Logging:
     * - Logs at the `INFO` level when the polling thread starts or stops.
     * - Logs at the `WARN` level if the thread is interrupted or if a polling thread is already active.
     * - Catches and logs any unexpected errors during the message consumption loop.
     */
    public void startPolling() {
        if (polling.compareAndSet(false, true)) {
            String parentThreadName = Thread.currentThread().getName();
            Thread pollingThread = new Thread(() -> {
                LOG.info("Started polling thread for topic: {} by parent thread: {}", topic, parentThreadName);
                try {
                    while (running) {
                        processRecords(pollMessages());
                        waitSeconds(2, "topic " + topic);
                    }
                } catch (Exception e) {
                    LOG.error("Error in polling thread for topic: {}", topic, e);
                } finally {
                    LOG.info("Stopped polling thread for topic: {}", topic);
                }
            }, parentThreadName + "-polling");
            setPollingWorkerThread(pollingThread);
            pollingThread.start();
        } else {
            LOG.warn("Polling thread is already running for topic: {}", topic);
        }
    }


    @Override
    public void processRecords(ConsumerRecords<Object, GenericRecord> records) {
        long timestamp = 0;
        synchronized (this) {
            if (records.count() != 0) {
                timestamp = Instant.now().toEpochMilli();
                LOG.debug("Processing {} records for topic: {} at timestamp: {}", records.count(), topic, timestamp);
            }
            for (ConsumerRecord<Object, GenericRecord> singleRecord : records) {
                try {
                    LOG.debug("Processing a record for timestamp: {} and record.ts {}, diff {}",
                            timestamp, singleRecord.timestamp(), timestamp - singleRecord.timestamp());
                    MessageMetadata metadata = extractMetadata(singleRecord, timestamp);
                    if (metadata != null) {
                        addToBuffer(metadata);
                    }
                } catch (Exception e) {
                    LOG.error("Error processing record: {}", singleRecord, e);
                }
            }
        }
    }


    /**
     * Extracts metadata from a given Kafka {@link ConsumerRecord}.
     * <p>
     * The method validates if the key of the record is of type {@link GenericData.Record},
     * then retrieves a specific "gkey" field from the record based on the topic.
     * If the "gkey" value is found and is of type {@link Long}, it constructs a
     * {@link MessageMetadata} object with the "gkey" value, its string representation,
     * and the timestamp from the record. If the key does not meet the expected
     * format, a warning is logged and null is returned.
     *
     * @param singleRecord the Kafka consumer record containing the key and value for
     *               which metadata is to be extracted
     * @return a {@link MessageMetadata} object containing the "gkey", its value,
     * and the record's timestamp if successfully extracted; otherwise null
     */
    private MessageMetadata extractMetadata(ConsumerRecord<Object, GenericRecord> singleRecord, long timestamp) {
        Object key = singleRecord.key();

        if (key instanceof GenericData.Record keyRecord) {
            Object gKeyObj = keyRecord.get(getGKeyNameByTopic(topic));
            if (gKeyObj instanceof Long gKeyValue) {
                return new MessageMetadata(gKeyValue.toString(), gKeyValue, timestamp);
            }
        }
        LOG.warn("Invalid key format for topic: {} - key: {}", topic, key);
        return null;
    }

    /**
     * Adds the given {@code MessageMetadata} object to the buffer if it is not already present.
     * The method ensures thread safety and prevents the addition of duplicate records based on the gKeyValue.
     * It logs a message indicating whether the record was added or skipped as duplicate.
     * After adding or skipping the record, it prints the current state of the buffer.
     *
     * @param metadata the {@code MessageMetadata} object to be added to the buffer
     */
    private synchronized void addToBuffer(MessageMetadata metadata) {
        boolean exists = buffer.stream().anyMatch(existing -> existing.gKeyValue().equals(metadata.gKeyValue()));
        if (!exists) {
            if (buffer.offer(metadata)) {
                LOG.debug("Added firsttime gkey to buffer: {}", metadata);
            } else {
                LOG.debug("Not added to buffer, {}", metadata);
            }
        } else {
            LOG.debug("Skipped duplicate gkey: {}", metadata);
        }
        printBuffer();
    }

    private String getGKeyNameByTopic(String topic) {
        return topic.toLowerCase().contains("inv_move_event") ? "mve_gkey" : "gkey";
    }

    /**
     * Removes all {@link MessageMetadata} records from the buffer that have a timestamp
     * earlier than the specified threshold. This method is synchronized to ensure thread safety.
     *
     * @param timestamp the cutoff timestamp; records with a timestamp earlier than this
     *                  value will be removed from the buffer
     */
    public synchronized void removeOlderGkeysFromBuffer(long timestamp) {
        List<MessageMetadata> retainedRecords = new ArrayList<>();
        buffer.drainTo(retainedRecords);
        retainedRecords.removeIf(metadata -> metadata.timestamp() < timestamp);
        buffer.addAll(retainedRecords);
        LOG.info("Removed older records. Buffer size: {}", buffer.size());
    }

    /**
     * Finds the timestamp associated with the given gKey in the buffer.
     * If a record with the given gKey is found, removes older records from the buffer
     * and returns the timestamp of the matched record. If no record is found, logs the
     * information and returns null. If the gKey cannot be parsed as a Long, an error
     * is logged and null is returned.
     *
     * @param gKey the string representation of the gKey to look up in the buffer
     * @return the timestamp associated with the gKey if found, or null otherwise
     */
    public synchronized Long findDateForGKey(String gKey) {
        try {
            Long gKeyValue = Long.parseLong(gKey);
            for (MessageMetadata metadata : buffer) {
                if (metadata.gKeyValue().equals(gKeyValue)) {
                    LOG.debug("Found record with gKeyValue = {}. Removing older records...", gKeyValue);
                    removeOlderGkeysFromBuffer(metadata.timestamp());
                    return metadata.timestamp();
                }
            }
            LOG.info("No record found for gKeyValue = {}", gKeyValue);
        } catch (NumberFormatException e) {
            LOG.error("Invalid gKey format: {}", gKey, e);
        }
        return null;
    }

    /**
     * Finds the last instance of a message in the buffer matching the provided gKey.
     * <p>
     * The method searches the buffer for a {@link MessageMetadata} object with a gKey value
     * that matches the parsed Long value of the provided gKey string. If found, it returns
     * the matching {@link MessageMetadata} object. If no matching record is found, logs an
     * informational message and returns null. In case the gKey cannot be parsed into a Long,
     * logs an error message and returns null.
     *
     * @param gKey the string representation of the gKey to search for in the buffer
     * @return the {@link MessageMetadata} object with the matching gKey, or null if no match is found
     */
    public MessageMetadata findLastGkeyInstance(String gKey) {
        try {
            Long gKeyValue = Long.parseLong(gKey);
            for (MessageMetadata metadata : buffer) {
                if (metadata.gKeyValue().equals(gKeyValue)) {
                    return metadata;
                }
            }
            LOG.info("No record found for gKeyValue = {}", gKeyValue);
        } catch (NumberFormatException e) {
            LOG.error("Invalid gKey format: {}", gKey, e);
        }
        return null;
    }

    private void printBuffer() {
        LOG.info("Buffer for topic {} contains {} records:", topic, buffer.size());

        buffer.stream()
                .sorted(Comparator.comparingLong(MessageMetadata::timestamp)
                        .reversed()
                        .thenComparingLong(MessageMetadata::gKeyValue)
                        .reversed())
                .limit(25)
                .forEach(singleRecord -> LOG.info(singleRecord.toString()));
    }


    public BlockingQueue<MessageMetadata> getBuffer() {
        return buffer;
    }

    public record MessageMetadata(String gKey, Long gKeyValue, Long timestamp) {

        @Override
        public String toString() {
            return "gKey='" + gKey + "', timestamp=" + timestamp;
        }
    }
}
