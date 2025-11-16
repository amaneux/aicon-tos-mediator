package com.aicon.tos.exceptions;

public class FieldNotFoundException extends RuntimeException {
    public FieldNotFoundException(String fieldName, String schemaName) {
        super("Field '" + fieldName + "' not found in schema: " + schemaName);
    }
}
