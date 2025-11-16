package com.aicon.tos.interceptor;

import com.avlino.common.MetaField;

/**
 * Represents a collected field with before/after values and change detection.
 */
public class InterceptorValueObject<T> {
    private final MetaField<T> field;
    private final Object beforeValue;
    private final Object afterValue;
    private final boolean created;
    private final boolean changed;
    private final boolean deleted;

    public InterceptorValueObject(MetaField<T> field, Object beforeValue, Object afterValue) {
        this.field = field;
        this.beforeValue = beforeValue;
        this.afterValue = afterValue;
        this.created = (beforeValue == null && afterValue != null);
        this.changed = (beforeValue == null || afterValue == null)
                ? beforeValue != afterValue // If one is null, it's a change
                : !beforeValue.equals(afterValue); // Compare values
        this.deleted = (beforeValue != null && afterValue == null);
    }

    // Getters
    public MetaField<?> field() {
        return field;
    }

    public Object beforeValue() {return beforeValue;}

    public String beforeValueAsString(String nullValue) {return beforeValue != null ? String.valueOf(beforeValue) : nullValue;}

    public Object afterValue() {
        return afterValue;
    }

    public String afterValueAsString(String nullValue) {return afterValue != null ? String.valueOf(afterValue) : nullValue;}

    public boolean isCreated() {
        return created;
    }

    public boolean isChanged() {
        return changed;
    }

    public boolean isDeleted() {
        return deleted;
    }

    @Override
    public String toString() {
        return String.format("InterceptorVO[field=%s, before=%s, after=%s, changed=%b]",
                field.id(), beforeValue, afterValue, changed);
    }

    public MetaField<T> getMetaField() {
        return this.field;
    }
}
