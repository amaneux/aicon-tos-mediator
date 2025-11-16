package com.aicon.tos.interceptor;

import com.aicon.TestConstants;
import com.aicon.tos.interceptor.newgenproducerconsumer.mock.InterceptorConsumerInterface;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InterceptorCollectTest {

    private static final String TEST_ENTITY = "inv_wi (TEST)";
    private InterceptorConsumerInterface consumerMock;
    private InterceptorCollect interceptorCollect;

    @BeforeEach
    void setUp() {
        ConfigSettings.setConfigFile(TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/interceptortest-connections.xml");
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup interceptorConfigGroup = config.getMainGroup(ConfigType.Interceptors);

        InterceptorConfig interceptorConfig = new InterceptorConfig(interceptorConfigGroup);

        consumerMock = mock(InterceptorConsumerInterface.class);
        interceptorCollect = new InterceptorCollect(interceptorConfig.getEntityConfig(TEST_ENTITY), consumerMock);
    }

    @Test
    void testCollectMessagesSkipsEmptyThenReturnsOne() {
        // Add this block:
        GenericRecord mockKey = mock(GenericRecord.class);
        when(mockKey.get(0)).thenReturn("1232123");

        ConsumerRecord<GenericRecord, GenericRecord> kafkaRecord = mock(ConsumerRecord.class);
        when(kafkaRecord.key()).thenReturn(mockKey); // Now mockKey is defined and mocked

        // Arrange
        ConsumerRecords<GenericRecord, GenericRecord> emptyRecords = new ConsumerRecords<>(Collections.emptyMap());
        ConsumerRecords<GenericRecord, GenericRecord> validRecords = buildSingleRecord();

        // Mock the behavior of the consumer to first return emptyRecords, then validRecords
        when(consumerMock.pollMessages())
                .thenReturn(emptyRecords)
                .thenReturn(validRecords);

        // Act
        List<CollectedMessage> messages = interceptorCollect.collectMessages();

        // Assert
        assertEquals(1, messages.size()); // Verify that 1 message is collected
    }


    @Test
    void testCollectMessagesWithMultipleFieldsAndRecords() {

        // Add this block:
        GenericRecord mockKey = mock(GenericRecord.class);
        when(mockKey.get(0)).thenReturn("1232123");

        // Create schema with multiple fields
        Schema.Field field1 = mock(Schema.Field.class);
        when(field1.name()).thenReturn("field1");
        when(field1.schema()).thenReturn(Schema.create(Schema.Type.STRING));

        Schema.Field field2 = mock(Schema.Field.class);
        when(field2.name()).thenReturn("field2");
        when(field2.schema()).thenReturn(Schema.create(Schema.Type.INT));

        Schema schema = mock(Schema.class);
        when(schema.getFields()).thenReturn(List.of(field1, field2));

        GenericRecord beforeRecord = mock(GenericRecord.class);
        GenericRecord afterRecord = mock(GenericRecord.class);
        when(afterRecord.getSchema()).thenReturn(schema);

        when(beforeRecord.get("field1")).thenReturn("a");
        when(afterRecord.get("field1")).thenReturn("b");

        when(beforeRecord.get("field2")).thenReturn(1);
        when(afterRecord.get("field2")).thenReturn(2);

        GenericRecord wrapperRecord = mock(GenericRecord.class);
        when(wrapperRecord.get("before")).thenReturn(beforeRecord);
        when(wrapperRecord.get("after")).thenReturn(afterRecord);

        ConsumerRecord<GenericRecord, GenericRecord> kafkaRecord = mock(ConsumerRecord.class);
        when(kafkaRecord.value()).thenReturn(wrapperRecord);
        when(kafkaRecord.key()).thenReturn(mockKey); // Now mockKey is defined and mocked

        TopicPartition partition = new TopicPartition("test-topic", 0);
        ConsumerRecords<GenericRecord, GenericRecord> records = new ConsumerRecords<>(
                Map.of(partition, List.of(kafkaRecord, kafkaRecord))  // 2 records
        );

        when(consumerMock.pollMessages()).thenReturn(records);

        List<CollectedMessage> messages = interceptorCollect.collectMessages();
        assertEquals(2, messages.size());  // Expect 2 messages since there are 2 records
        assertEquals(2, messages.get(0).getChangedFields().size());  // 2 changed fields for the first record
        assertEquals(2, messages.get(1).getChangedFields().size());  // 2 changed fields for the second record
    }

    private ConsumerRecords<GenericRecord, GenericRecord> buildSingleRecord() {
        Schema.Field field = mock(Schema.Field.class);
        when(field.name()).thenReturn("field1");
        when(field.schema()).thenReturn(Schema.create(Schema.Type.STRING));

        GenericRecord mockKey = mock(GenericRecord.class);
        when(mockKey.get(0)).thenReturn("1232123"); // Mock the return of key.get(0)

        Schema schema = mock(Schema.class);
        when(schema.getFields()).thenReturn(List.of(field));

        GenericRecord beforeRecord = mock(GenericRecord.class);
        GenericRecord afterRecord = mock(GenericRecord.class);
        when(afterRecord.getSchema()).thenReturn(schema);

        when(beforeRecord.get("field1")).thenReturn("old");
        when(afterRecord.get("field1")).thenReturn("new");

        GenericRecord wrapperRecord = mock(GenericRecord.class);
        when(wrapperRecord.get("before")).thenReturn(beforeRecord);
        when(wrapperRecord.get("after")).thenReturn(afterRecord);

        ConsumerRecord<GenericRecord, GenericRecord> kafkaRecord = mock(ConsumerRecord.class);
        when(kafkaRecord.value()).thenReturn(wrapperRecord);
        when(kafkaRecord.key()).thenReturn(mockKey);

        TopicPartition partition = new TopicPartition("test-topic", 0);

        return new ConsumerRecords<>(Map.of(partition, List.of(kafkaRecord)));
    }
}
