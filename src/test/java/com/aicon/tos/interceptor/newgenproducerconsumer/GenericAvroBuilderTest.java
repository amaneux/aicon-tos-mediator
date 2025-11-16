package com.aicon.tos.interceptor.newgenproducerconsumer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GenericAvroBuilderTest {

    @Test
    void testBuildMessageWithValidSchemaAndValues() {
        // Arrange
        String schemaStr = """
                {
                    "type": "record",
                    "name": "TestRecord",
                    "fields": [
                        {"name": "field1", "type": "string"}
                    ]
                }
                """;

        Schema schema = new Schema.Parser().parse(schemaStr);
        Map<String, Object> fieldValues = Map.of("field1", "value1");

        // Act
        GenericRecord record = GenericAvroBuilder.buildMessage(schema, fieldValues);

        // Assert
        assertNotNull(record);
        assertEquals("value1", record.get("field1"));
    }

    @Test
    void testBuildMessageHandlesNullValues() {
        // Arrange
        String schemaStr = """
                {
                    "type": "record",
                    "name": "TestRecord",
                    "fields": [
                        {"name": "field1", "type": ["null", "string"], "default": null}
                    ]
                }
                """;

        Schema schema = new Schema.Parser().parse(schemaStr);
        Map<String, Object> fieldValues = Collections.emptyMap();

        // Act
        GenericRecord record = GenericAvroBuilder.buildMessage(schema, fieldValues);

        // Assert
        assertNotNull(record);
        assertNull(record.get("field1"), "Field value should be null when not provided");
    }

    @Test
    void testBuildMessageHandlesNestedRecords() {
        // Arrange
        String nestedSchemaStr = """
                {
                    "type": "record",
                    "name": "NestedRecord",
                    "fields": [
                        {"name": "nestedField", "type": "string"}
                    ]
                }
                """;

        String parentSchemaStr = """
                {
                    "type": "record",
                    "name": "ParentRecord",
                    "fields": [
                        {"name": "parentField", "type": {"type": "record", "name": "NestedRecord", "fields": [
                            {"name": "nestedField", "type": "string"}
                        ]}}
                    ]
                }
                """;

        Schema nestedSchema = new Schema.Parser().parse(nestedSchemaStr);
        Schema parentSchema = new Schema.Parser().parse(parentSchemaStr);

        Map<String, Object> nestedValues = Map.of("nestedField", "nestedValue");
        Map<String, Object> parentValues = Map.of("parentField", nestedValues);

        // Act
        GenericRecord parentRecord = GenericAvroBuilder.buildMessage(parentSchema, parentValues);

        // Assert
        assertNotNull(parentRecord);
        GenericRecord nestedRecord = (GenericRecord) parentRecord.get("parentField");
        assertNotNull(nestedRecord);
        assertEquals("nestedValue", nestedRecord.get("nestedField"));
    }

    @Test
    void testBuildMessageHandlesArrays() {
        // Arrange
        String schemaStr = """
        {
            "type": "record",
            "name": "ArrayRecord",
            "fields": [
                {"name": "arrayField", "type": {"type": "array", "items": "string"}}
            ]
        }
        """;

        Schema schema = new Schema.Parser().parse(schemaStr);
        Map<String, Object> fieldValues = Map.of("arrayField", List.of("value1", "value2"));

        // Act
        GenericRecord record = GenericAvroBuilder.buildMessage(schema, fieldValues);

        // Assert
        assertNotNull(record);
        List<?> arrayData = (List<?>) record.get("arrayField");
        assertNotNull(arrayData);
        assertEquals(2, arrayData.size());
        assertTrue(arrayData.contains("value1"));
        assertTrue(arrayData.contains("value2"));
    }

    @Test
    void testBuildMessageThrowsForInvalidSchema() {
        // Arrange
        Schema schema = Schema.create(Schema.Type.NULL); // Invalid schema for a record
        Map<String, Object> fieldValues = Map.of("field1", "value1");

        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () ->
                GenericAvroBuilder.buildMessage(schema, fieldValues));
        assertTrue(exception.getMessage().contains("Record schema is required"));
    }
}