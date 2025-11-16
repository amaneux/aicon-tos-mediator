package com.aicon.tos.exceptions;

/**
 * Exception for server-side HTTP errors (5xx).
 */
public class HttpServerException extends HttpException {
    public HttpServerException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }
}