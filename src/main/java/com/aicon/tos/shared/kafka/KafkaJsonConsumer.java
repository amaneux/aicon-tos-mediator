package com.aicon.tos.shared.kafka;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static com.aicon.tos.shared.kafka.KafkaConfig.aicon_dispatch_itv_job_resequence_request_topic;

public class KafkaJsonConsumer {

    public static void main(String[] args) {
        // Kafka Consumer configuratie
        Properties props = KafkaConfig.getConsumerProps();
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
                  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
                  "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "latest"); // Start from first message

        KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
        consumer.subscribe(Collections.singleton(aicon_dispatch_itv_job_resequence_request_topic));

        try {
            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println("Received JSON message: " + record.value());
                }
            }
        } finally {
            consumer.close();
        }
    }
}
