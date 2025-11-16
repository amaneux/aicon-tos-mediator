package com.aicon.tos.shared.kafka;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.interceptor.newgenproducerconsumer.mock.InterceptorConsumerInterface;
import org.apache.avro.generic.GenericRecord;

import java.util.Properties;


public class InterceptorConsumer extends KafkaConsumerBase<GenericRecord, GenericRecord> implements InterceptorConsumerInterface {

    public InterceptorConsumer(String topic) {
        super(ConfigDomain.prefixIfNeeded(topic));

        // Initialize the Kafka consumer with the properties specified in KafkaConfig
        Properties consumerProps = KafkaConfig.getConsumerProps();
        consumerProps.setProperty("specific.avro.reader", "false");
        this.initializeConsumer(consumerProps);
    }
}
