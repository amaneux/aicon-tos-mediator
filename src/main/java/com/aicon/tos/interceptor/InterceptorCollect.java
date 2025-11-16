package com.aicon.tos.interceptor;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.interceptor.newgenproducerconsumer.mock.InterceptorConsumerInterface;
import com.aicon.tos.interceptor.newgenproducerconsumer.mock.MockInterceptorConsumer;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import com.aicon.tos.shared.kafka.InterceptorConsumer;
import com.avlino.common.MetaField;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Collects messages from 1 specific CDC-topic when requested.
 */
public class InterceptorCollect {
    private static final Logger LOG = LoggerFactory.getLogger(InterceptorCollect.class);

    public static final String ELM_BEFORE = "before";
    public static final String ELM_AFTER = "after";

    private InterceptorConsumerInterface interceptorConsumer;
    private InterceptorEntityConfig entityConfig;

    public InterceptorCollect(InterceptorEntityConfig entityConfig) {
        this.entityConfig = entityConfig;

        interceptorConsumer = entityConfig.useMockedInterceptor()
                ? new MockInterceptorConsumer(entityConfig.getTopicName())
                : new InterceptorConsumer(entityConfig.getTopicName());

        LOG.info("Started for topic {}", entityConfig.getTopicName());
    }

    public ConnectorProgress getConnectorStatus() {
        return interceptorConsumer.getStatus();
    }

    /**
     * Test Constructor with test config and mock consumer
     * @param entityConfig
     * @param mockInterceptorConsumer
     */
    protected InterceptorCollect(InterceptorEntityConfig entityConfig, InterceptorConsumerInterface mockInterceptorConsumer) {
        //test constructor
        this(entityConfig);
        this.interceptorConsumer = mockInterceptorConsumer;
    }

    private static Class<?> avroTypeToJavaClass(Schema.Type avroType) {
        switch (avroType) {
            case STRING:
                return String.class;
            case INT:
                return Integer.class;
            case LONG:
                return Long.class;
            case FLOAT:
                return Float.class;
            case DOUBLE:
                return Double.class;
            case BOOLEAN:
                return Boolean.class;
            case BYTES:
                return ByteBuffer.class;
            default:
                return Object.class; // or throw if you want to be strict
        }
    }

    /**
     * Collects a single message from the topic.
     * This method replaces queue-based polling and directly returns a processed message.
     *
     * @return The collected message or `null` if interrupted or no message available.
     */
    public List<CollectedMessage> collectMessages() {
        List<CollectedMessage> collectedMessages = new ArrayList<>();

        ConsumerRecords<GenericRecord, GenericRecord> records;

        String entityName = entityConfig.getEntityName();
        String topic = entityConfig.getTopicName();
        LOG.trace("Start polling topic {}/{}", entityName, topic);
        do {
            records = interceptorConsumer.pollMessages();
            if (!records.isEmpty()) {
                boolean first = true;
                for (ConsumerRecord<GenericRecord, GenericRecord> rec : records) {
                    if (first) {
                        LOG.info("Collecting {} records starting with offset {} from {}/{}", records.count(), rec.offset(), entityName, topic);
                        first = false;
                    }

                    // Create a new fields list for each record
                    Map<String, InterceptorValueObject<?>> fieldsMap = new LinkedHashMap<>();

                    try {
                        String key = String.valueOf(rec.key().get(0));
                        GenericRecord beforeValue = (GenericRecord) rec.value().get(ELM_BEFORE);
                        GenericRecord afterValue = (GenericRecord) rec.value().get(ELM_AFTER);

                        CDCAction action = CDCAction.getAction(beforeValue == null, afterValue == null);

                        Schema schema = afterValue != null ? afterValue.getSchema() : beforeValue.getSchema();
                        for (Schema.Field field : schema.getFields()) {
                            String fieldname = field.name();

                            MetaField<?> metaField = new MetaField<>(fieldname, avroTypeToJavaClass(field.schema().getType()));

                            fieldsMap.put(fieldname,
                                    new InterceptorValueObject<>(
                                            metaField,
                                            beforeValue != null ? beforeValue.get(fieldname) : null,
                                            afterValue != null ? afterValue.get(fieldname) : null)
                            );
                        }
                        collectedMessages.add(new CollectedMessage(action, entityName, rec.offset(), rec.timestamp(), key, fieldsMap));
                    } catch (Exception e) {
                        LOG.error("Processing message for topic {}/{} failed, reason: {}", entityName, topic, e.getMessage());
                    }
                }
            }
        } while (records.isEmpty());        // todo ron do we really need this loop here? We should parse all received records and then return or not?

        return collectedMessages;
    }
}
