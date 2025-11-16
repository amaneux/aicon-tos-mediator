package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.aicon.tos.interceptor.newgenproducerconsumer.testcode.KafkaAdmin;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import com.aicon.tos.shared.kafka.KafkaConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

/**
 * Abstract base class for Kafka generic consumers.
 * This class provides a framework for consuming messages from a Kafka topic
 * and processing them using a user-implemented message handling method.
 *
 * @param <K> the type of the message key
 * @param <V> the type of the message value, which must extend GenericRecord
 */
@Deprecated
public abstract class KafkaGenericConsumerBase<K, V extends GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaGenericConsumerBase.class);

    private KafkaConsumer<K, V> consumer;
    protected final String topic;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final ConnectorProgress status;
    Properties consumerProps = null;
    boolean weCreateThisTopic;

    protected KafkaGenericConsumerBase(String topic, String differentGroupId, boolean weCreateThisTopic) {
        this.topic = topic;
        this.status = new ConnectorProgress(getName());
        this.weCreateThisTopic = weCreateThisTopic;

        consumerProps = KafkaConfig.getConsumerProps();
        consumerProps.put(KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, "false");

        if (differentGroupId != null) {
            consumerProps.put(ConsumerConfig.GROUP_ID_CONFIG, differentGroupId);
        }
        isConnected();
    }

    protected KafkaGenericConsumerBase(String topic, boolean weCreateThisTopic) {
        this(topic, null, weCreateThisTopic);
    }

    public boolean isConnected() {
        if (status.getState().isConnected()) {
            return true;
        }
        try {
            if (status.is(IDLE)) {
                status.setProgress(INITIALISING);
            }
            if (consumer == null) {
                KafkaConfig.logProperties("consumer", topic, consumerProps);
                consumer = new KafkaConsumer<>(consumerProps);
                consumer.subscribe(List.of(this.topic));
                LOG.info("{} created and subscribed to topic: {}", getName(), topic);
            }

            if (KafkaAdmin.getInstance().isKafkaReachable()) {
                running.set(true);
                status.setProgress(INITIALIZED).resetResult();
                return true;
            } else {
                String text = String.format("Not connected to Kafka broker %s (check VPN, brokers, etc)",
                        consumerProps.getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
                status.setResult(ResultLevel.ERROR, text);
                return false;
            }
        } catch (Exception e) {
            String text = String.format("Initialisation of %s failed, reason: %s", getName(), e.getMessage());
            LOG.error(text);
            status.setResult(ResultLevel.ERROR, text);
            return false;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public ConsumerRecords<K, V> poll(Duration wait) {
        return consumer.poll(wait);
    }

    public void close() {
        stop();
        consumer.close();
    }

    public ConnectorProgress getStatus() {
        return status;
    }

    /**
     * Stops the consumer loop and closes the Kafka consumer.
     */
    public void stop() {
        status.setProgress(STOPPING, ResultLevel.OK, "Requested by user");
        LOG.info("Stopping consumer for topic: {}", topic);
        running.set(false);
        if (consumer != null) {
            consumer.wakeup(); // only interrupt poll()
        }
    }

    public String getTopic() {
        return topic;
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    /**
     * Implement this method to process consumed message.
     */
    public abstract void handleMessage(ConsumerRecord<K, V> message);
}
