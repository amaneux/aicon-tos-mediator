
package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockInterceptorMain {
    private static final Logger LOG = LoggerFactory.getLogger(MockInterceptorMain.class);

    public static void main(String[] args) {
        String topic = "hvr.inv_wi";
        MockInterceptorConsumer consumer = new MockInterceptorConsumer(topic);

        ConsumerRecords<GenericRecord, GenericRecord> records = consumer.pollMessages();
        for (ConsumerRecord<GenericRecord, GenericRecord> singleRecord : records) {
            LOG.info("Received mock record:");
            LOG.info(String.valueOf(singleRecord));
        }
    }
}

