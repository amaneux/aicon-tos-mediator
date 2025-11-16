package com.aicon.tos.interceptor.newgenproducerconsumer;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility class for building Avro GenericRecord instances based on given schemas and field data.
 * This class supports handling complex schema structures including messages, arrays, and unions.
 */
public class GenericAvroBuilder {

    private GenericAvroBuilder() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Builds a {@link GenericRecord} instance by extracting the schema
     * associated with the given topic from the schema registry and populating it with the supplied field values.
     *
     * @param topic       the name of the topic for which to retrieve the schema
     * @param fieldValues a map of field names and their corresponding values to populate the message
     * @return a {@link GenericRecord} populated with the provided field values
     */
    public static GenericRecord buildMessage(String topic, Map<String, Object> fieldValues) {
        Schema schema = SchemaLoader.getValueSchemaFromRegistry(topic);

        return buildMessage(schema, fieldValues);
    }

    /**
     * Builds a {@link GenericRecord} instance based on the provided Avro schema and field values.
     * This method recursively processes nested messages and arrays to ensure all fields
     * are properly populated according to the schema definition.
     *
     * @param schema      the Avro schema defining the structure of the message
     * @param fieldValues a map containing the field names as keys and their corresponding
     *                    values to populate the message; nested structures like messages and arrays
     *                    should be represented as maps or lists respectively
     * @return a {@link GenericRecord} populated with the supplied field values
     */
    public static GenericRecord buildMessage(Schema schema, Map<String, Object> fieldValues) {
    if (schema == null || schema.getType() != Schema.Type.RECORD) {
        throw new IllegalArgumentException("Record schema is required");
    }

        GenericRecord message = new GenericData.Record(schema);

        for (Schema.Field field : schema.getFields()) {
            Object value = fieldValues.get(field.name());

            Schema fieldSchema = field.schema();
            Schema.Type fieldType = getNonNullableType(fieldSchema);

            if (value == null) {
                message.put(field.name(), null);
                continue;
            }

            switch (fieldType) {
                case RECORD -> {
                    Map<String, Object> nestedValues = (Map<String, Object>) value;
                    // Pass the field's schema, not the parent schema
                    message.put(field.name(), buildMessage(fieldSchema, nestedValues));
                }
                case ARRAY -> {
                    Schema elementSchema = getNonNullableSchema(fieldSchema).getElementType();

                    if (elementSchema.getType() == Schema.Type.RECORD) {
                        // Handle arrays of records
                        List<Map<String, Object>> elements = (List<Map<String, Object>>) value;
                        List<GenericRecord> nestedList = new ArrayList<>();
                        for (Map<String, Object> item : elements) {
                            nestedList.add(buildMessage(elementSchema, item));
                        }
                        message.put(field.name(), nestedList);
                    } else {
                        // Handle arrays of primitive types
                        List<?> elements = (List<?>) value;
                        message.put(field.name(), elements);
                    }
                }

                default -> message.put(field.name(), value);
            }
        }
        return message;
    }


    /**
     * Retrieves the non-nullable type from the given Avro schema.
     * If the schema is a union type, it searches for the first non-null type within the union.
     * Otherwise, it directly returns the schema's type.
     *
     * @param schema the Avro schema from which the non-nullable type is to be extracted
     * @return the non-nullable {@link Schema.Type} of the given schema
     */
    private static Schema.Type getNonNullableType(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            return schema.getTypes().stream()
                    .filter(s -> s.getType() != Schema.Type.NULL)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No non-null type in union"))
                    .getType();
        } else {
            return schema.getType();
        }
    }

    /**
     * Retrieves the non-nullable schema from the given Avro schema.
     * If the schema is of type UNION, it filters out the null type and returns the first non-null type
     * within the union. If no non-null type exists, an exception is thrown.
     * For non-UNION schemas, it directly returns the schema itself.
     *
     * @param schema the Avro schema from which the non-nullable schema is to be extracted
     * @return the non-nullable {@link Schema} derived from the provided schema
     * @throws RuntimeException if the schema is of type UNION and does not contain a non-null type
     */
    private static Schema getNonNullableSchema(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            return schema.getTypes().stream()
                    .filter(s -> s.getType() != Schema.Type.NULL)
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("No non-null schema in union"));
        }
        return schema;
    }
}
