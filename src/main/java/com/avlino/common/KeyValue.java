package com.avlino.common;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;

import static com.avlino.common.Constants.EMPTY;

/**
 * Simple class for storing a key,value pair.
 * At construction you have to provide a type for the value if you don't want to use the default String.class.
 */
public class KeyValue<T> implements Comparable<KeyValue>, Serializable {
    private static final Logger LOG = LoggerFactory.getLogger(ValueObject.class);

    public static final String ATT_KEY = "key";
    public static final String ATT_VALUE = "value";

    protected String key;
    protected Class<T> type;
    protected T value = null;

    public KeyValue(String key) {
        this(key, (Class<T>) String.class);
    }

    public KeyValue(String key, Class<T> type) {
        this.key = key;
        this.type = type;
    }

    public KeyValue(String key, Class<T> type, T value) {
        this(key, type);
        this.value = value;
    }

    public KeyValue(KeyValue param) {
        this(param.key, param.type);
        setValue((T)param);
    }

    public String key() {
        return key;
    }

    public KeyValue setKey(String key) {
        this.key = key;
        return this;
    }

    public <T> T value() {
        return valueIfNull(null);
    }

    public <T> T valueIfNull(Object defValue) {
        Object retValue = this.value == null ? defValue : this.value;
        if (retValue == null) {
            return null;
        } else if (type.isInstance(retValue)) {
            return (T) type.cast(retValue);
        }
        LOG.warn("Value={}({}) is not of type {}", value, value != null ? value.getClass().getSimpleName() : null, type);
        return null;        // throw new IllegalArgumentException(text);
    }

    public String valueAsString() {
        return valueAsString(null);
    }

    public String valueAsString(String nullDefault) {
        if (value == null) {
            return nullDefault;
        } else if (value instanceof String) {
            return (String)value;
        } else {
            return String.valueOf(value);
        }
    }

    /**
     * @return true when not null and in case of a String also !isEmpty();
     */
    public boolean hasValue() {
        return !valueAsString(EMPTY).isEmpty();
    }

    public KeyValue setValue(T value) {
        this.value = value;
        return this;
    }

    @Override
    public int compareTo(@NotNull KeyValue other) {
        if (this == other) return 0;
        if (other == null) {
            throw new NullPointerException(String.format("Comparing %s to other == null", this));
        }
        return key.compareTo(other.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof KeyValue)) return false;
        return key.equals(((KeyValue)other).key);
    }

    public String toString() {
        return String.format("(key=%s(%s), value=%s)", key, type.getClass().getSimpleName(),  value);
    }
}
