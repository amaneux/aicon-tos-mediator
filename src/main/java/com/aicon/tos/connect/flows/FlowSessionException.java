package com.aicon.tos.connect.flows;

public class FlowSessionException extends Exception {
    public FlowSessionException(String message) {
        super(message);
    }

    public FlowSessionException(String message, Throwable cause) {
        super(message, cause);
    }
}
