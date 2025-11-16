package com.aicon.tos.shared.kafka;

import com.aicon.tos.interceptor.newgenproducerconsumer.testcode.KafkaAdmin;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaException;
import org.apache.kafka.common.errors.InvalidConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aicon.tos.shared.ResultLevel.ERROR;
import static com.aicon.tos.shared.ResultLevel.WARN;
import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

public class AbstractKafkaProducerBase<K, V> {

    protected KafkaProducer<K, V> producer;
    protected String topic;
    protected final Logger LOG = LoggerFactory.getLogger(getClass());
    protected ConnectorProgress status;

    private void createProducerWhenNull() {
        // your logic for lazy initialization
    }

    public void sendMessage(ProducerRecord<K, V> message, Callback callback) {
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

    public void sendMessage(K key, V value, Callback callback) {
        ProducerRecord<K, V> message = new ProducerRecord<>(topic, key, value);
        sendMessage(message, callback);
    }

    public void sendMessage(K key, V message) throws KafkaException {
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
                    status.setResult(ResultLevel.ERROR, msg);
                    LOG.error(msg);
                }
            });

            if (status.getResultLevel() == ERROR) {
                throw new KafkaException(status.getResultMessage());
            }
        }
    }

    private String getName() {
        return this.getClass().getSimpleName();
    }
}
