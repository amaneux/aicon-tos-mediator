package com.aicon.tos.exceptions;

/**
 * General exception for HTTP-related issues.
 */
public class HttpException extends RuntimeException {
    private final int statusCode;
    private final String responseBody;

    public HttpException(String message, int statusCode, String responseBody) {
        super(message);
        this.statusCode = statusCode;
        this.responseBody = responseBody;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getResponseBody() {
        return responseBody;
    }
}

