package com.aicon.tos.connect.cdc;

/**
 * Defines the action happened to the CDC record
 */
public enum CDCAction {
    CREATED("+"),
    DELETED("-"),
    CHANGED("*");

    private String symbol;

    private CDCAction(String symbol) {
        this.symbol= symbol;
    }

    public static CDCAction getAction(boolean isCreated, boolean isDeleted) {
        if (isCreated) {return CREATED;}
        if (isDeleted) {return DELETED;}
        return CHANGED;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isCreated() {
        return this == CREATED;
    }

    public boolean isChanged() {
        return this == CHANGED;
    }

    public boolean isDeleted() {
        return this == DELETED;
    }
}
