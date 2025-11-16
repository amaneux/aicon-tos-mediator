package com.aicon.tos.shared.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;

public class KafkaTest {
    private static final Logger LOG = LoggerFactory.getLogger(KafkaTest.class);

    private static final String TOPIC = "tos.TN4USER.INV_WI_MV";
    private static final String GROUP = "test-group";//"connect-mongo-sink-inv_wi_mv";

    public static void main(String[] args) {
        // Consume messages from the topic
        consumeMessages();
    }

    private static void consumeMessages() {
        Properties consumerProps = KafkaConfig.getConsumerProps();
        consumerProps.setProperty("group.id", GROUP);
        consumerProps.setProperty("specific.avro.reader", "false");
        consumerProps.setProperty("auto.offset.reset", "latest");
        consumerProps.setProperty("key.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");
        consumerProps.setProperty("value.deserializer", "io.confluent.kafka.serializers.KafkaAvroDeserializer");

        BufferedKafkaConsumer consumer = new BufferedKafkaConsumer(TOPIC);
        consumer.initializeConsumer(consumerProps);
        consumer.startPolling();

        String tosGkey = "3439926059";
        try {
            while (true) {
                BufferedKafkaConsumer.MessageMetadata tosMetadata = consumer.getBuffer().stream().findAny().orElse(null);
                if (tosMetadata != null) {
                    tosGkey = tosMetadata.gKey();
                }
                BufferedKafkaConsumer.MessageMetadata metadata = consumer.findLastGkeyInstance(tosGkey);
                if (metadata != null) {
                    LOG.info("Latest metadata for {}: {}", tosGkey, metadata);
                } else {
                    LOG.info("No new message in the buffer.");
                }
               waitSeconds(5,"");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error while consuming messages", e);
        } finally {
            consumer.stop();
        }
    }
}
