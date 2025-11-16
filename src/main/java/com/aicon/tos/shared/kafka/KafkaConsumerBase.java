package com.aicon.tos.shared.kafka;

import com.aicon.tos.interceptor.newgenproducerconsumer.testcode.KafkaAdmin;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.errors.InterruptException;
import org.apache.kafka.common.errors.WakeupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

/**
 * Base class for Kafka consumer.
 * todo RON : Review this merged class with KafkaGenericConsumerBase
 *
 * @param <K> the key type
 * @param <V> the value type
 */
public abstract class KafkaConsumerBase<K, V extends GenericRecord> {
    public static final int CONNECTION_RETRY_DELAY_MSEC = 10 * 1000;
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerBase.class);
    private static final long WARNING_DURATION_SEC = 60;
    protected final ConnectorProgress status;
    protected final AtomicReference<Thread> pollingWorkerThread = new AtomicReference<>(); // Thread-safe reference for managing polling thread
    private final Object consumerLock = new Object(); // Lock for thread-safe access
    protected volatile boolean running = true;
    protected String topic;
    boolean weCreateThisTopic;
    private KafkaConsumer<K, V> consumer;
    private Properties properties;
    private Instant lastMessageReceiveAt = null;

    protected KafkaConsumerBase(String topic) {
        this(topic, false);
    }

    protected KafkaConsumerBase(String topic, boolean weCreateThisTopic) {
        this(topic, null, weCreateThisTopic);
    }

    protected KafkaConsumerBase(String topic, String differentGroupId, boolean weCreateThisTopic) {
        this.topic = topic;
        this.status = new ConnectorProgress(getName());
        this.weCreateThisTopic = weCreateThisTopic;

        properties = KafkaConfig.getConsumerProps();
        properties.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");

        if (differentGroupId != null) {
            properties.put(ConsumerConfig.GROUP_ID_CONFIG, differentGroupId);
        }
        isConnected();
    }

    public void initializeConsumer(Properties properties) {
        this.properties = properties;
        isConnected();
    }

    public boolean isConnected() {
        if (status.getState().isConnected()) {
            return true;
        }
        if (status.is(IDLE)) {
            status.setProgress(INITIALISING);
        }
        synchronized (consumerLock) {
            try {
                if (consumer == null) {
                    KafkaConfig.logProperties("consumer", topic, properties);
                    consumer = new KafkaConsumer<>(properties);
                    consumer.subscribe(List.of(topic));
                    LOG.info("{} created and subscribed to topic: {}", getName(), topic);
                }
                if (KafkaAdmin.getInstance().isKafkaReachable()) {
                    running = true;
                    status.setProgressWhen(INITIALIZED, INITIALISING).resetResult();
                    return true;
                } else {
                    running = false;
                    String text = String.format("Not connected to Kafka broker %s (check VPN, brokers, etc)",
                            properties.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
                    status.setResult(ResultLevel.ERROR, text);
                    return false;
                }
            } catch (Exception e) {
                String text = String.format("Init failed for %s consumer. Check VPN, brokers (%s)", topic, e.getMessage());
                status.setResult(ResultLevel.ERROR, text);
                LOG.error(text);
                return false;
            }
        }
    }

    public ConsumerRecords<K, V> poll(Duration pollTimeout) {
        return pollMessages();
    }

    public ConsumerRecords<K, V> pollMessages() {
        status.setProgressWhen(CONNECTING, INITIALIZED);
        ConsumerRecords<K, V> msg = ConsumerRecords.empty();
        try {
            if (isConnected()) {
                if (LOG.isTraceEnabled()) LOG.trace("Polling for topic: {}", topic);
                msg = consumer.poll(Duration.ofMillis(KafkaConfig.POLL_WAIT_IN_MSEC));
                if (msg.count() > 0 || checkConnectionOk()) {
                    status.setProgress(CONNECTED).resetResult();
                }
                if (msg.count() > 0) {
                    lastMessageReceiveAt = Instant.now();
                    LOG.info("Found {} messages in topic {}", msg.count(), topic);
                } else {
                    if (LOG.isTraceEnabled()) LOG.trace("Found 0 messages in topic {}", topic);
                }
            } else {
                status.setResult(ResultLevel.WARN, String.format("Not running yet, retry after %s ms", CONNECTION_RETRY_DELAY_MSEC));
                Thread.sleep(CONNECTION_RETRY_DELAY_MSEC);
            }
            return msg;
        } catch (WakeupException e) {
            if (!running) {
                LOG.info("Polling interrupted for topic: {}, consumer is shutting down.", topic);
            } else {
                LOG.error("Unexpected wakeup exception during polling for topic: {}", topic, e);
            }
        } catch (InterruptedException e) {
            if (running && !Thread.currentThread().isInterrupted()) {
                LOG.error("Unexpected Sleep InterruptedException during polling for topic: {}", topic, e);
            }
        } catch (InterruptException e) {
            if (running && !Thread.currentThread().isInterrupted()) {
                status.setProgress(RECONNECTING, ResultLevel.WARN, "Kafka interrupted...");
                LOG.error("Unexpected Kafka InterruptException during polling for topic: {}", topic, e);
            }
        }
        LOG.info("Return from pollMessages() with empty records from {}.", topic);
        return ConsumerRecords.empty();
    }

    public void stop() {
        LOG.info("Stopping consumer for topic: {}", topic);
        status.setProgress(STOPPING, ResultLevel.OK, "Requested by user");
        running = false;

        synchronized (consumerLock) {
            if (consumer != null) {
                consumer.wakeup();
            }
        }

        Thread workerThread = pollingWorkerThread.get(); // Get the current thread atomically
        if (workerThread != null && workerThread.isAlive()) {
            try {
                workerThread.join(5000); // Wait for thread to terminate
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while waiting for polling thread to stop for topic: {}", topic);
            }
        }

        synchronized (consumerLock) {
            if (consumer != null) {
                consumer.close(Duration.ofMillis(CONNECTION_RETRY_DELAY_MSEC));
                LOG.info("Consumer for topic {} closed successfully.", topic);
            }
        }
        status.setProgress(STOPPED);
    }

    public void setPollingWorkerThread(Thread newThread) {
        pollingWorkerThread.set(newThread); // Safely update the polling thread
    }

    public String getTopic() {
        return this.topic;
    }

    public ConnectorProgress getStatus() {
        return this.status;
    }

    public void close() {
        this.consumer.close();
    }

    public String getGroupId() {
        return this.consumer.groupMetadata().groupId();
    }


    private boolean checkConnectionOk() {
        String connectionStatus = StringUtils.EMPTY;
        try {
            if (lastMessageReceiveAt == null) {
                lastMessageReceiveAt = Instant.now();
            }

            Duration timeout = Duration.between(lastMessageReceiveAt.plusSeconds(WARNING_DURATION_SEC), Instant.now());

            if (timeout.isPositive() && timeout.toSeconds() % WARNING_DURATION_SEC == 0) {  // only do verifications once every ...
                if (KafkaAdmin.getInstance().isKafkaReachable()) {
                    connectionStatus = String.format("%s: no messages received since %s seconds.",
                            getName(), WARNING_DURATION_SEC + timeout.toSeconds());
                    status.setResult(ResultLevel.WARN, connectionStatus);
                    LOG.warn(connectionStatus);
                } else {
                    status.setProgress(RECONNECTING, ResultLevel.WARN, "Kafka unreachable");
                }
            }
            return timeout.isNegative();
        } catch (Exception e) {
            connectionStatus = e.getMessage();
            status.setResult(ResultLevel.ERROR, connectionStatus);
            return false;
        }
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public void processRecords(ConsumerRecords<K, V> records) {
    }

    public boolean isRunning() {
        return running;
    }
}