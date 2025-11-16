package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconYardDeckingUpdateResponseMessage;
import com.aicon.tos.shared.util.AnsiColor;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class AiconYardDeckingUpdateResponseProducer extends KafkaProducerBase<String, GenericRecord> {

    private static final Logger LOG = LoggerFactory.getLogger(AiconYardDeckingUpdateResponseProducer.class);

    public AiconYardDeckingUpdateResponseProducer() {
        super(KafkaConfig.getFlowResponseTopic("DeckingUpdateFlow"), true);
        ProducerManager.registerProducer(this);
    }

    public void sendAiconYardDeckingUpdateResponseMessage(String id) {
        AiconYardDeckingUpdateResponseMessage message = new AiconYardDeckingUpdateResponseMessage();
        try {
            //.......
        } catch (Exception e) {
            LOG.error(AnsiColor.red("Field does not exist in schema: {}"), e.getMessage(), e);
            throw e;
        }
        sendMessage(id, message);
    }
}
