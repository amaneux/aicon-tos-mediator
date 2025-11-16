package com.aicon.tos.shared.kafka;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ProducerManager {
    private static final Logger LOG = LoggerFactory.getLogger(ProducerManager.class);

    private static final List<KafkaProducerBase<?, ?>> producers = new ArrayList<>();

    public static void registerProducer(KafkaProducerBase<?, ?> producer) {
        LOG.info("Add producer for topic: {}",producer.getTopicName());
        producers.add(producer);
    }

    public static void closeAllProducers() {
        for (KafkaProducerBase<?, ?> producer : producers) {
            LOG.info("Close producer for topic: {}",producer.getTopicName());
            producer.close();
        }
        producers.clear();
    }
}