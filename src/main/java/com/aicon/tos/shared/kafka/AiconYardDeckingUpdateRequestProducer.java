package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import org.apache.avro.generic.GenericRecord;

public class AiconYardDeckingUpdateRequestProducer extends KafkaProducerBase<String, GenericRecord> {

    public AiconYardDeckingUpdateRequestProducer() {
        super(KafkaConfig.getFlowRequestTopic("DeckingUpdateFlow"), true);
        ProducerManager.registerProducer(this);
    }

    public void sendTosDeckingUpdateMessage(AiconYardDeckingUpdateRequestMessage message) {
        sendMessage((String) message.getRequestId(), message);
    }
}
