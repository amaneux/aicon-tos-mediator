package com.aicon.tos.shared;

public enum ResultLevel {
    OK,
    WARN,
    ERROR;

    public boolean isLower(ResultLevel other) {
        return this.ordinal() < other.ordinal();
    }
}
