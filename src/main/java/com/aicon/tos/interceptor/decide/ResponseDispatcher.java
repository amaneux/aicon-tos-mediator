package com.aicon.tos.interceptor.decide;

import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

public class ResponseDispatcher<V extends GenericRecord> implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseDispatcher.class);
    private final KafkaConsumerBase<GenericRecord, V> consumer;
    private final Map<String, ResponseHandler<V>> handlers = new ConcurrentHashMap<>();
    private final ConnectorProgress status = new ConnectorProgress();


    public ResponseDispatcher(KafkaConsumerBase<GenericRecord, V> consumer) {
        this.consumer = consumer;
    }

    public KafkaConsumerBase<GenericRecord, V> getConsumer() {
        return consumer;
    }

    public void registerHandler(String key, ResponseHandler<V> handler) {
        handlers.put(key, handler);
        LOG.debug("Registered handler for key: {}", key);
    }

    public void start() {
        consumer.getStatus().setProgressWhen(CONNECTING, INITIALIZED);
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                if (consumer.isConnected()) {
                    ConsumerRecords<GenericRecord, V> records = consumer.poll(Duration.ofMillis(1000));
                    consumer.getStatus().setProgress(CONNECTED).resetResult();

                    if (records.isEmpty()) continue;

                    LOG.debug("Received {} records from topic {}", records.count(), consumer.getTopic());

                    processReceiveRecords(records);
                } else {
                    consumer.getStatus()
                            .setProgressWhen(RECONNECTING, CONNECTED)
                            .setResult(ResultLevel.WARN, String.format("Retry after %s ms", KafkaConfig.RETRY_POLL_WAIT_IN_MSEC));
                    try {
                        Thread.sleep(KafkaConfig.RETRY_POLL_WAIT_IN_MSEC);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }

        } finally {
            try {
                status.setProgress(STOPPING);
                if (consumer != null) {
                    consumer.close();
                    LOG.info("Kafka consumer closed for topic: {}", consumer.getTopic());
                }
                status.setProgress(STOPPED);
            } catch (Exception e) {
                String topic = consumer != null ? consumer.getTopic() : "unknown";
                LOG.error("Error while closing Kafka consumer for topic: {}", topic, e);
                status.setProgress(STOPPED, new ResultEntry(ResultLevel.ERROR, e.getMessage()));
            }
            LOG.info("Stopped polling loop for topic: {}", consumer != null ? consumer.getTopic() : "unknown");
        }
    }

    private void processReceiveRecords(ConsumerRecords<GenericRecord, V> records) {
        for (ConsumerRecord<GenericRecord, V> genRecord : records) {
            Object key = genRecord.key();

            String responseKey;
            if (key instanceof GenericRecord genericKey) {
                responseKey = genericKey.get("request_index").toString();
            } else responseKey = key.toString();

            V responseValue = genRecord.value();
            LOG.debug("Processing record with key: {}", responseKey);

            ResponseHandler<V> handler = handlers.get(responseKey);
            if (handler != null) {
                LOG.debug("Completing future for key: {}", responseKey);
                handler.completeIfMatch(responseKey, responseValue);
                handlers.remove(responseKey);
            } else {
                LOG.info("No handler found for key: {}", responseKey);
                LOG.info("Handlers in map: {}", handlers);
            }
        }
    }
}
