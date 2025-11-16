package com.avlino.common;

import java.io.Serializable;
import java.util.logging.Logger;

public class ValueObject<T> implements Serializable {
    private static final Logger LOG = Logger.getLogger(ValueObject.class.getName());

    private final MetaField<T> field;
    private T value = null;

    public ValueObject(MetaField<T> metaField) {
        this.field = metaField;
        this.value = null;
    }

    @SuppressWarnings("unchecked")
    public ValueObject(MetaField<T> metaField, Object value) {
        this.field = metaField;
        try {
            this.value = (T) value;
        } catch (ClassCastException e) {
            LOG.warning("Invalid cast for field: " + metaField.id() + " with value: " + value);
            this.value = null;
        }
    }

    public String valueAsString() {
        return valueAsString(null);
    }

    public String valueAsString(String nullDefault) {
        return value == null ? nullDefault : String.valueOf(value);
    }

    public MetaField<T> field() {
        return field;
    }

    public T value() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s", value);
    }

    public ValueObject<T> set(T oldValue) {
        this.value = oldValue;
        return this;
    }
}
