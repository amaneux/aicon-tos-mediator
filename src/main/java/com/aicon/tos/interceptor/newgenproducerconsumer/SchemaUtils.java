package com.aicon.tos.interceptor.newgenproducerconsumer;

import org.apache.avro.Schema;

import java.util.List;

public class SchemaUtils {

    public static String getRequiredFields(Schema schema) {
        return getRequiredFields(schema, "");
    }

    private static String getRequiredFields(Schema schema, String indent) {
        StringBuilder builder = new StringBuilder();

        if (schema.getType() == Schema.Type.RECORD) {
            builder.append(indent).append("Record: ").append(schema.getName()).append("\n");

            List<Schema.Field> fields = schema.getFields();
            for (Schema.Field field : fields) {
                Schema fieldSchema = field.schema();
                if (!isNullable(fieldSchema)) {
                    builder.append(indent).append("  ").append(field.name()).append(" : ").append(fieldSchema.getType()).append("\n");

                    if (fieldSchema.getType() == Schema.Type.RECORD) {
                        builder.append(getRequiredFields(fieldSchema, indent + "    "));
                    }
                }
            }
        }
        return builder.toString();
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
}
