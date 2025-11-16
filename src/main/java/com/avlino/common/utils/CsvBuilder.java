package com.avlino.common.utils;

import com.avlino.common.MetaField;
import com.avlino.common.ValueObject;

import java.util.List;

import static com.avlino.common.Constants.COMMA;
import static com.avlino.common.Constants.DOUBLE_QUOTE;
import static com.avlino.common.Constants.EMPTY;
import static com.avlino.common.Constants.LF;

/**
 * Builds a CSV as a String from lists of MetaFields and ValueObjects.
 */
public class CsvBuilder {
    static public final String DELIMITER = COMMA;
    private static final String NULL_DEFAULT = EMPTY;

    private String delimiter = DELIMITER;
    private StringBuilder csv;

    public CsvBuilder() {
        csv = new StringBuilder();
    }

    public CsvBuilder(String delimiter) {
        this();
        this.delimiter = delimiter;
    }

    public void addColumnRow(List<? extends MetaField> fields) {
        for (int i = 0; i < fields.size(); i++) {
            MetaField field = fields.get(i);
            csv.append(field.id());
            if (i < fields.size() - 1) {
                csv.append(delimiter);
            }
        }
        csv.append(LF);
    }

    public void addDataRow(List<ValueObject> values) {
        for (int i = 0; i < values.size(); i++) {
            ValueObject value = values.get(i);
            csv.append(escapeValue(value.valueAsString(NULL_DEFAULT)));
            if (i < values.size() - 1) {
                csv.append(delimiter);
            }
        }
        csv.append(LF);
    }

    public String toString() {
        return csv.toString();
    }

    private String escapeValue(String data) {
        String escapedData = data;
        if (data.contains(delimiter) || data.contains(DOUBLE_QUOTE) || data.contains(LF)) {
            escapedData = DOUBLE_QUOTE + data.replaceAll(DOUBLE_QUOTE, "\"\"") + DOUBLE_QUOTE;
        }
        return escapedData;
    }
}
