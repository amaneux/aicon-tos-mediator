package com.avlino.common;

import jakarta.validation.constraints.NotNull;

import java.io.Serializable;

public class MetaField<T> implements Comparable<MetaField>, Serializable {

    private String id;
    private Class<T> type;

    public MetaField(
            String id,
            Class<T> type
    ) {
        this.id = id;
        this.type = type;
    }


    public Class<T> type() {
        return type;
    }

    public String codeGen(String indent, String prefix) {
        return String.format("%spublic static final %s %s%s = new %s(\"%s\", %s.class);",
                indent,
                MetaField.class.getSimpleName(),
                prefix, id.toUpperCase(),
                MetaField.class.getSimpleName(),
                id.toLowerCase(),
                type.getName());
    }

    public String id() {
        return id;
    }

    public String toString() {
        return String.format("%s(%s)", id, type.getSimpleName());
    }


    @Override
    public int compareTo(@NotNull MetaField other) {
        if (this == other) return 0;
        if (other == null) {
            throw new NullPointerException(String.format("Comparing %s to other == null", this));
        }
        return id.compareTo(other.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }


    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (other == null || !(other instanceof MetaField)) return false;
        return id.equals(((MetaField)other).id);
    }
}
