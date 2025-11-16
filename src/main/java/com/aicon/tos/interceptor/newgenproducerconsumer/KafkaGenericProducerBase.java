package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.interceptor.newgenproducerconsumer.testcode.KafkaAdmin;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import com.aicon.tos.shared.kafka.KafkaConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.aicon.tos.shared.ResultLevel.ERROR;
import static com.aicon.tos.shared.ResultLevel.WARN;
import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

/**
 * Base class for creating and managing Kafka producers with Avro GenericRecord messages.
 * <p>
 * Provides functionality to initialize a Kafka producer, send messages using either a
 * fully formed ProducerRecord or simply by providing a key and value, and closing the producer.
 * Ensures proper resource management and logs relevant information or errors during operations.
 * todo yusuf merge this with KafkaProducerBase into a single class or have the Generic one extending the Base (to much similarities between these 2).
 *
 * @param <K> The type of the key for Kafka messages.
 * @param <V> The type of the value, constrained to extend GenericRecord.
 */
public abstract class KafkaGenericProducerBase<K, V extends GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(KafkaGenericProducerBase.class);

    protected KafkaProducer<GenericRecord, GenericRecord> producer;
    private KafkaAdmin kafkaAdmin = KafkaAdmin.getInstance();
    protected String topic;
    Properties producerProps = null;
    protected final ConnectorProgress status;
    private boolean propAllowTopicCreation;
    private boolean allowTopicCreation;

    public KafkaGenericProducerBase(String topic, boolean allowTopicCreation) {
        this.topic = topic;
        status = new ConnectorProgress(getName());
        this.allowTopicCreation = allowTopicCreation;       // todo should only come from our xml-config

        producerProps = KafkaConfig.getProducerProps();
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        KafkaConfig.logProperties("producer", topic, producerProps);

        // ProducerConfig doesn't have this property, therefore, we look at the ConsumerConfig one and use that value
        propAllowTopicCreation = Boolean.valueOf(producerProps.getProperty(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG));

        createProducerWhenNull();
    }

    void createProducerWhenNull() {
        try {
            if (producer == null) {
                if (kafkaAdmin.isKafkaReachable()) {  // we can only answer the questions below when kafka is reachable.
                    status.setProgress(INITIALISING);
                    if (allowTopicCreation || kafkaAdmin.topicExistsExactly(topic)) {
                        producer = new KafkaProducer<>(KafkaConfig.getProducerProps());
                        status.setProgressWhen(INITIALIZED, INITIALISING).resetResult();
                        LOG.info("Kafka producer created for topic: {}", topic);
                    } else {
                        String text = String.format("Kafka topic %s not created because we are not allowed to", topic);
                        status.setProgress(FAILED, ERROR, text);
                        LOG.error(text);
                    }
                } else {
                    status.setResult(WARN, "Kafka not reachable yet, check VPN, broker url");
                }
            } // else we stay IDLE until we have an answer
        } catch (Exception e) {
            String text = String.format("Failed to create Kafka producer for topic: %s", this.topic);
            status.setResult(ResultLevel.ERROR, text);
            LOG.error(text);
        }
    }

    /**
     * Send a message using a fully constructed ProducerRecord.
     */
    public void sendMessage(ProducerRecord<GenericRecord, GenericRecord> message, Callback callback) {
        if (message == null || callback == null) {
            throw new IllegalArgumentException("Record and callback must not be null.");
        }

        createProducerWhenNull();
        if (producer == null) {
            throw new IllegalStateException("Kafka producer is not initialized.");
        }
        LOG.info("Sending message with key: {} to topic: {}", message.key(), topic);
        status.setProgressWhen(CONNECTING, INITIALIZED);
        try {
            producer.send(message, callback);
            status.setProgress(CONNECTED).resetResult();
        } catch (Exception e) {
            String msg = String.format("Failed to send message for %s (VPN available?): %s", getName(), e.getMessage());
            status.setResult(ResultLevel.ERROR, msg);
            LOG.error(msg);
            throw e;
        }
    }

    /**
     * Convenience method: send a message using just a key and value.
     */
    public void sendMessage(K key, V value, Callback callback) {
        ProducerRecord<K, V> message = new ProducerRecord<>(topic, key, value);
        sendMessage((ProducerRecord<GenericRecord, GenericRecord>) message, callback);
    }

    public ConnectorProgress getStatus() {
        return status;
    }

    /**
     * Close the producer and release all resources.
     */
    public void close() {
        status.setProgress(STOPPING);
        if (producer != null) {
            try {
                producer.close();
                producer = null;
                status.setProgress(STOPPED);
                LOG.info("Kafka producer for topic: {} closed.", topic);
            } catch (Exception e) {
                LOG.error("Error while closing Kafka producer for topic: {}", topic, e);
            }
        }
    }

    public String getName() {
        return getClass().getSimpleName();
    }

    public String getTopicName() {
        return topic;
    }
}
