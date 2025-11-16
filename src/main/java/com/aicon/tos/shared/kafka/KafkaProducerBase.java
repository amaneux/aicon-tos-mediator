package com.aicon.tos.shared.kafka;

import com.aicon.tos.interceptor.newgenproducerconsumer.testcode.KafkaAdmin;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aicon.tos.shared.ResultLevel.ERROR;
import static com.aicon.tos.shared.ResultLevel.WARN;
import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

/**
 * Abstract base class for producing messages to a Kafka topic.
 * Provides common functionality for sending Avro-based messages to Kafka.
 *
 * @param <K> The type of the key for Kafka messages.
 * @param <V> The type of the value for Kafka messages, extending {@link GenericRecord}.
 */
public abstract class KafkaProducerBase<K, V extends GenericRecord> {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerBase.class);

    protected KafkaProducer<K, V> producer;
    protected String topic;
    protected ConnectorProgress status;
    private boolean propAllowTopicCreation;
    private boolean allowTopicCreation;
    private KafkaAdmin kafkaAdmin = KafkaAdmin.getInstance();

    public KafkaProducerBase(String topic, boolean allowTopicCreation) {
        this.topic = topic;

        //ProducerConfig doesn't have this property, therefore, we look at the ConsumerConfig one and use that value
        String property = KafkaConfig.getProducerProps().get(ConsumerConfig.ALLOW_AUTO_CREATE_TOPICS_CONFIG).toString();
        this.propAllowTopicCreation = Boolean.valueOf(property);
        this.allowTopicCreation = allowTopicCreation;

        status = new ConnectorProgress(getName());
        KafkaConfig.logProperties("producer", topic, KafkaConfig.getProducerProps());
        createProducerWhenNull();
    }

    private KafkaProducer createProducerWhenNull() {
        try {
            if (producer == null) {    // we can only answer the questions below when kafka is reachable.
                if (kafkaAdmin.isKafkaReachable()) {
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
            status.setResult(ERROR, String.format("Failed to create %s for topic: ", getName(), topic));
            LOG.error("Failed to initialize Kafka producer for topic {}, reason: {}", topic, e.getMessage());
        }
        return producer;
    }


    public void sendMessage(
            K key,
            V message
    ) throws KafkaException {
        createProducerWhenNull();
        if (producer != null) {
            status.setProgressWhen(CONNECTING, INITIALIZED);
            ProducerRecord<K, V> producerRecord = new ProducerRecord<>(topic, key, message);
            LOG.info("Sending message with key: {} to topic: {}", key, topic);
            producer.send(producerRecord, (metadata, exception) -> {
                if (exception == null) {
                    status.setProgress(CONNECTED).resetResult();
                    LOG.info("Message successfully sent to topic: {} with key: {}", topic, key);
                } else {
                    String msg;
                    if (exception instanceof InvalidConfigurationException) {
                        msg = "Configuration error";
                    } else {
                        msg = "VPN, kafka broker?";
                    }
                    msg = String.format("Failed to send message for %s (%s): %s", getName(), msg, exception.getMessage());
                    status.setResult(ERROR, msg);
                    LOG.error(msg);
                }
            });
            if (status.getResultLevel() == ERROR) {
                throw new KafkaException(status.getResultMessage());
            }
        }
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

    public String getNewId() {
        return String.valueOf(System.currentTimeMillis());
    }
}
