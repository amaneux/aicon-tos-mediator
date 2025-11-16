package com.aicon.tos.shared.exceptions;

// New exception class introduced for clarity and specificity
public class EmailSendingException extends RuntimeException {
    public EmailSendingException(String message, Throwable cause) {
        super(message, cause);
    }
}