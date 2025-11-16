package com.aicon.tos.exceptions;

/**
 * Exception for client-side HTTP errors (4xx).
 */
public class HttpClientException extends HttpException {
    public HttpClientException(String message, int statusCode, String responseBody) {
        super(message, statusCode, responseBody);
    }
}