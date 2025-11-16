package com.aicon.tos.shared.kafka;

import com.avlino.aicon.WorkQueueActivationRequest.dispatch_work_queue_activation_key;
import com.avlino.aicon.WorkQueueActivationRequest.dispatch_work_queue_activation_value;
import com.avlino.aicon.WorkQueueActivationRequest.requests;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;

public class AiconTosConnectWQRequestTopicProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectWQRequestTopicProducer.class);

    public AiconTosConnectWQRequestTopicProducer() {
        super(KafkaConfig.aicon_dispatch_work_queue_activation_request_topic, false);
        ProducerManager.registerProducer(this);
    }

    public void sendAiconTosConnectRequestTopicMessage(String request_id) {

        dispatch_work_queue_activation_key msgKey = new dispatch_work_queue_activation_key();
        msgKey.setRequestIdx(request_id);
        msgKey.setApiIdentifier(14);
        msgKey.setTimestamp(Instant.now());

        dispatch_work_queue_activation_value msgValue = new dispatch_work_queue_activation_value();
        msgValue.setRequestIdx(msgKey.getRequestIdx());
        msgValue.setAPIIdentifier(msgKey.getApiIdentifier());
        msgValue.setTimestamp(msgKey.getTimestamp());
        try {
            requests.Builder request = requests.newBuilder()
                    .setVesselVisitId("VV001")
                    .setQCId("QC53")
                    .setWorkQueueId("QC53-LOAD")
                    .setUptUserId("DGL")
                    .setProgramId("interface");

            msgValue.setRequests(Collections.singletonList(request.build()));

        } catch (IllegalArgumentException e) {
            LOG.error("Invalid request for request_id: {}", request_id);
            throw e;
        }

        sendMessage(msgKey.toString(), msgValue);
    }
}