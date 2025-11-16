package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.interceptor.newgenproducerconsumer.AiconDeckingConfig;
import com.aicon.tos.shared.kafka.KafkaConsumerBase;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.*;

public class AiconDeckingEngineMockup extends KafkaConsumerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingEngineMockup.class);
    private final AiconDeckingResponseProducer responseProducer;
    private final Random random = new Random();

    public AiconDeckingEngineMockup() {
        super(AiconDeckingConfig.getAiconDeckingRequestTopic(), false);
        this.responseProducer = new AiconDeckingResponseProducer();
    }

    private static String toNullableString(Object obj) {
        return (obj != null) ? obj.toString() : null;
    }

    private static boolean toBoolean(Object obj) {
        return obj != null && Boolean.parseBoolean(obj.toString());
    }

    @Override
    public void processRecords(ConsumerRecords<String, GenericRecord> records) {
        if (records.count() != 0) {
            long timestamp = Instant.now().toEpochMilli();
            LOG.debug("Processing {} records for topic: {} at timestamp: {}", records.count(), topic, timestamp);
        }

        records.forEach(record -> processRecord(record));
    }

    private void processRecord(ConsumerRecord<String, GenericRecord> receivedMessage) {
        GenericRecord value = receivedMessage.value();
        String key = receivedMessage.key();
        LOG.info("Received request with key: {} => {}", key, value);

        // Extract first request (assumes only one for simplicity)
        var receivedRequests = (Iterable<GenericRecord>) value.get("requests");
//        GenericRecord item = requests.iterator().next();

        // Randomly introduce errors
        Integer errorCode = null;
        String errorDesc = null;
        if (random.nextDouble() < 0.2) { // 20% chance
            errorCode = 500;
            errorDesc = "Simulated error";
        }

        List<Map<String, Object>> responseRequests = new ArrayList<>();
        for (GenericRecord receivedRequest : receivedRequests) {

            // Map fields to flat structure
            Map<String, Object> responseRequest = new HashMap<>();
            responseRequest.put("containerNumber", toNullableString(receivedRequest.get("containerNumber")));
            responseRequest.put("containerVisitId", toNullableString(receivedRequest.get("containerVisitId")));
            responseRequest.put("jobType", toNullableString(receivedRequest.get("jobType")));
            responseRequest.put("uptUserId", toNullableString(receivedRequest.get("uptUserId")));
            responseRequest.put("programId", toNullableString(receivedRequest.get("programId")));
            responseRequest.put("blockIndexNumber", toNullableString(receivedRequest.get("blockIndexNumber")));
            responseRequest.put("bayIndexNumber", toNullableString(receivedRequest.get("bayIndexNumber")));
            responseRequest.put("rowIndexNumber", toNullableString(receivedRequest.get("rowIndexNumber")));
            responseRequest.put("tierIndexNumber", toNullableString(receivedRequest.get("tierIndexNumber")));
            responseRequest.put("internalIndex1", toNullableString(receivedRequest.get("internalIndex1")));
            responseRequest.put("internalIndex2", toNullableString(receivedRequest.get("internalIndex2")));
            responseRequest.put("requestedBlockOnly", toBoolean(receivedRequest.get("requestedBlockOnly")));
            responseRequest.put("isEmptyContainer", toBoolean(receivedRequest.get("isEmptyContainer")));
            responseRequest.put("errorCode", errorCode);
            responseRequest.put("errorDesc", errorDesc);

            responseRequests.add(responseRequest);
        }

        GenericRecord response = responseProducer.createMessage(key, responseRequests, errorCode, errorDesc);
        responseProducer.sendMessage(response);
    }

    public void close() {
        responseProducer.close();
    }
}
