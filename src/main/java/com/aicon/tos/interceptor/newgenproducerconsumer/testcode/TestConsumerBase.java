package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

import static com.aicon.tos.shared.connectors.ConnectorProgress.ConnectorState.*;

public class TestConsumerBase {
    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingRequestConsumerMain.class);
    //    protected KafkaGenericConsumerBase consumer = null;
    protected KafkaConsumerBase consumer = null;

    /**
     * Starts the polling loop. Messages are processed via `handleRecord()`.
     * Runs in the calling thread.
     */
    public void startPollingLoop() {
        if (consumer == null) {
            LOG.error("Consumer not created, create it first");
            return;
        }

        LOG.info("Started polling loop for topic: {}", consumer.getTopic());
        consumer.getStatus().setProgressWhen(CONNECTING, INITIALIZED);
        try {
            while (consumer.isRunning()) {
                try {
                    ConsumerRecords<String, GenericRecord> messages = consumer.poll(Duration.ofSeconds(1));
                    consumer.getStatus().setProgress(CONNECTED).resetResult();
                    consumer.processRecords(messages);
                } catch (Exception e) {
                    consumer.getStatus().setProgress(RECONNECTING, new ResultEntry(ResultLevel.ERROR,
                            String.format("Retry after %s ms (%s)",
                                    KafkaConfig.RETRY_POLL_WAIT_IN_MSEC, e.getMessage())).setException(e));
                    try {
                        Thread.sleep(KafkaConfig.RETRY_POLL_WAIT_IN_MSEC);
                    } catch (InterruptedException ex) {
                        throw new RuntimeException(ex);
                    }
                    if (consumer.isRunning()) {
                        LOG.error("Exception while polling topic {}: {}", consumer.getTopic(), e.getMessage(), e);
                    }
                }
            }
        } finally {
            try {
                consumer.getStatus().setProgress(STOPPING);
                if (consumer != null) {
                    consumer.close();
                    LOG.info("Kafka consumer closed for topic: {}", consumer.getTopic());
                }
                consumer.getStatus().setProgress(STOPPED);
            } catch (Exception e) {
                LOG.error("Error while closing Kafka consumer for topic: {}, {}", consumer.getTopic(), e.getMessage());
                consumer.getStatus().setProgress(STOPPED, new ResultEntry(ResultLevel.ERROR, e.getMessage()));
            }
            LOG.info("Stopped polling loop for topic: {}", consumer.getTopic());
        }
    }
}
