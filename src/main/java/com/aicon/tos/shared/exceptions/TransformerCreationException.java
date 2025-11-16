package com.aicon.tos.shared.exceptions;

/**
 * Exception thrown when a RequestResponseTransformer cannot be created.
 */
public class TransformerCreationException extends Exception {

    public TransformerCreationException(String message) {
        super(message);
    }

    public TransformerCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
