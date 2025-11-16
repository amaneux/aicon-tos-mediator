package com.aicon.tos.interceptor.newgenproducerconsumer;

import org.apache.avro.Schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SchemaFieldExtractor {

    public static Set<String> extractRequiredFieldNames(Schema schema) {
        Set<String> requiredFields = new HashSet<>();
        for (Schema.Field field : schema.getFields()) {
            if (!isNullable(field.schema())) {
                requiredFields.add(field.name());
            }
        }
        return requiredFields;
    }

    public static Set<String> extractOptionalFieldNames(Schema schema) {
        Set<String> optionalFields = new HashSet<>();
        for (Schema.Field field : schema.getFields()) {
            if (isNullable(field.schema())) {
                optionalFields.add(field.name());
            }
        }
        return optionalFields;
        }

    public static Map<String, Set<String>> typeToFields(Schema schema) {
        Map<String, Set<String>> result = new HashMap<>();

        for (Schema.Field field : schema.getFields()) {
            String key = fieldKey(field);
            result.computeIfAbsent(key, k -> new HashSet<>()).add(field.name());
    }

        return result;
    }

    private static String fieldKey(Schema.Field field) {
        Schema baseSchema = unwrapNullableUnion(field.schema());
        String typePrefix = isNullable(field.schema()) ? "OPTIONAL" : "REQUIRED";
        return typePrefix + "_FIELDS_" + baseSchema.getType().name();
    }


    public static Schema unwrapNullableUnion(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            for (Schema s : schema.getTypes()) {
                if (s.getType() != Schema.Type.NULL) {
                    return s;
                }
            }
        }
        return schema;
    }

    public static Object getDefaultValue(Schema.Field field) {
        Schema fieldSchema = unwrapNullableUnion(field.schema());
        if (field.defaultVal() != null) {
            return field.defaultVal();
        }
        if (isNullable(field.schema())) {
            return null;
        }
        switch (fieldSchema.getType()) {
            case BOOLEAN:
                return false;
            case INT:
            case LONG:
                return 0;
            case FLOAT:
            case DOUBLE:
                return 0.0;
            case STRING:
                return "";
            case RECORD:
                return new Object(); // You can improve this if you want nested support
            default:
                return null;
        }
    }

    private static boolean isNullable(Schema schema) {
        if (schema.getType() == Schema.Type.UNION) {
            for (Schema s : schema.getTypes()) {
                if (s.getType() == Schema.Type.NULL) {
                    return true;
                }
            }
        }
        return false;
    }

    public static Schema.Field getFieldByName(Schema schema, String fieldName) {
        return schema.getField(fieldName);
    }
    }
