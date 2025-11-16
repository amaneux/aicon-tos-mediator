package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.shared.connectors.ConnectorProgress;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public interface InterceptorConsumerInterface {

    public abstract ConsumerRecords<GenericRecord, GenericRecord> pollMessages();

    void close();

    String getTopic();

    ConnectorProgress getStatus();
}

