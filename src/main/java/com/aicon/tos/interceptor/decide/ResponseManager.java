package com.aicon.tos.interceptor.decide;


import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import java.util.concurrent.CompletableFuture;

public class ResponseManager<V extends GenericRecord> {
    private final ResponseDispatcher<V> dispatcher;

    public ResponseManager(KafkaConsumerBase<GenericRecord, V> consumer) {
        this.dispatcher = new ResponseDispatcher<>(consumer);
        this.dispatcher.start();
    }


    public KafkaConsumerBase<GenericRecord, V> getConsumer() {
        return dispatcher.getConsumer();
    }


    public String getTopicName() {
        return dispatcher.getConsumer().getTopic();
    }


    public CompletableFuture<V> registerAndGetFuture(String key) {
        ResponseHandler<V> handler = new ResponseHandler<>(key);
        dispatcher.registerHandler(key, handler);
        return handler.getFuture();
    }
}
