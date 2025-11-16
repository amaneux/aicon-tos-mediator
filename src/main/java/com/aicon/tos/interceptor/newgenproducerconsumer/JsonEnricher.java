package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Field;

public class JsonEnricher {

    private static Schema valueSchema = null;

    public static ObjectNode enrichJson(Schema schema, ObjectNode input) {

        if (valueSchema == null) {
            valueSchema = SchemaFieldExtractor.unwrapNullableUnion(
                    SchemaFieldExtractor.getFieldByName(schema, "before").schema()
            );
        }
        ObjectNode enriched = JsonNodeFactory.instance.objectNode();

        for (Field field : valueSchema.getFields()) {
            String name = field.name();

            if (name.equals("before") || name.equals("after")) {
                if (input.has(name)) {
                    ObjectNode wrapped = JsonNodeFactory.instance.objectNode();
                    wrapped.set(valueSchema.getFullName(), input.get(name));
                    enriched.set(name, wrapped);
                } else {
                    enriched.putNull(name);
                }
            } else {
                if (input.has(name)) {
                    enriched.set(name, input.get(name));
                } else {
                    enriched.putNull(name);
                }
            }
        }

        return wrapInUnion(valueSchema.getFullName(), enriched);
    }

    private static ObjectNode wrapInUnion(String typeName, ObjectNode content) {
        ObjectNode wrapper = JsonNodeFactory.instance.objectNode();
        wrapper.set(typeName, content);
        return wrapper;
    }

    private static Schema extractValueSchema(Schema topLevelSchema) {
        for (Field field : topLevelSchema.getFields()) {
            if (field.name().equals("after") || field.name().equals("before")) {
                Schema fieldSchema = field.schema();
                if (fieldSchema.getType() == Schema.Type.UNION) {
                    for (Schema unionType : fieldSchema.getTypes()) {
                        if (unionType.getType() == Schema.Type.RECORD && unionType.getName().equals("Value")) {
                            return unionType;
                }
            }
                }
            }
        }
        throw new IllegalArgumentException("Could not find 'Value' record in 'after' or 'before'");
    }

    private static Schema getNonNullType(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            return schema.getTypes().stream()
                    .filter(s -> s.getType() != Schema.Type.NULL)
                    .findFirst()
                    .orElse(schema);
        }
        return schema;
    }
}
