package com.aicon.tos.shared.exceptions;

public class SoapResponseTransformationException extends RuntimeException {
    public SoapResponseTransformationException(String message) {
        super(message);
    }

    public SoapResponseTransformationException(String message, Throwable cause) {
        super(message, cause);
    }
}
