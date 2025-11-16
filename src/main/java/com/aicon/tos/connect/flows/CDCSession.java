package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.kafka.BufferedKafkaConsumer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;

/**
 * The CDCSession class is responsible for managing a session that interacts
 * with a Kafka topic for Change Data Capture (CDC) operations.
 * It extends the FlowSession class and provides specific implementation
 * for CDC use cases, including Kafka consumer initialization and processing.
 */
public class CDCSession extends FlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(CDCSession.class);

    protected CDCController cdcController;
    BufferedKafkaConsumer consumer;

    public CDCSession(CDCController cdcController, BufferedKafkaConsumer consumer) {
        super(null, "CDC_SESSION", null, null, null);
        this.cdcController = cdcController;
        this.consumer = consumer;

        LOG.info("Instantiate CDC session for topic {}", consumer.getTopic());
    }

    public CDCSession(CDCController controller, String topic) {
        this(controller, new BufferedKafkaConsumer(ConfigDomain.prefixIfNeeded(topic)));
    }

    /**
     * Executes the CDC session for the specified Kafka topic.
     * <p>
     * This method is responsible for initializing a Kafka consumer with specific properties,
     * starting the polling process, and maintaining the processing loop while the session
     * is active. The session's state is updated at the beginning of the method using the
     * CDC controller, and the session is terminated gracefully when the `running` flag
     * is set to false. Logs are used to indicate the start and end of the session.
     * <p>
     * Functionality includes:
     * - Configuring and initializing a Kafka consumer with predefined settings.
     * - Polling messages from the Kafka topic using a BufferedKafkaConsumer.
     * - Managing session state and updating it via the CDCController instance.
     * - Supporting graceful shutdown by stopping the consumer and invoking
     * appropriate cleanup methods at the end of the session.
     * <p>
     * The method also handles thread interruption using a try-catch block
     * during polling intervals. If an interruption occurs, a {@link RuntimeException}
     * is thrown to ensure proper error reporting.
     */
    @Override
    protected void execute() {
        LOG.info("Starting CDC session for topic {}.", consumer.getTopic());

        Properties consumerProps = KafkaConfig.getConsumerProps();
        consumerProps.setProperty("specific.avro.reader", "false");
        consumerProps.setProperty("auto.offset.reset", KafkaConfig.DEFAULT_AUTO_OFFSET_RESET);
        consumerProps.setProperty("key.deserializer", KafkaAvroDeserializer.class.getName());
        consumerProps.setProperty("value.deserializer",  KafkaAvroDeserializer.class.getName());

        consumer.initializeConsumer(consumerProps);
        consumer.startPolling();

        while (this.isRunning()) {
            waitSeconds(2, "CDCSession");
        }
        consumer.stop();
        cdcController.sessionEnded(this);
        LOG.info("CDC session finished");
        LOG.info("-------------");
    }

    public Long findDateForGKey(String gKey) {
        return consumer.findDateForGKey(gKey);
    }

    public void stopSession() {
        LOG.info("Stopping CDCSession for topic {}.", consumer.getTopic());
        setRunning(false);
    }

    public String getTopic() {
        return consumer.getTopic();
    }

    public void setConsumer(BufferedKafkaConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    protected String getThreadName(int threadCounter) {
        return String.format("CDCSession-%s", consumer.getTopic());
    }
}
