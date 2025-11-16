package com.aicon.tos.connect.http;

import com.aicon.tos.exceptions.HttpClientException;
import com.aicon.tos.exceptions.HttpException;
import com.aicon.tos.exceptions.HttpServerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.http.HttpResponse;

public class HttpResponseHandler {
    private static final Logger LOG = LoggerFactory.getLogger(HttpResponseHandler.class);

    private HttpResponseHandler() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Processes the HTTP response and throws specific exceptions for client
     * and server errors.
     *
     * @param response The HTTP response to process.
     * @return The response body if the response is successful (2xx).
     */
    public static String handleHttpResponse(HttpResponse<String> response) {
        int statusCode = response.statusCode();
        String responseBody = response.body();

        if (statusCode >= 200 && statusCode <= 299) {
            // Return response body for successful requests.
            return responseBody.trim();
        } else if (statusCode >= 400 && statusCode <= 499) {
            // Log and throw client error exception.
            LOG.error("Client error: statusCode={}, responseBody={}", statusCode, responseBody);
            throw new HttpClientException("Client-side error occurred.", statusCode, responseBody);
        } else if (statusCode >= 500 && statusCode <= 599) {
            // Log and throw server error exception.
            LOG.error("Server error: statusCode={}, responseBody={}", statusCode, responseBody);
            throw new HttpServerException("Server-side error occurred.", statusCode, responseBody);
        } else {
            // Unexpected or other status codes.
            LOG.error("Unexpected error: statusCode={}, responseBody={}", statusCode, responseBody);
            throw new HttpException("Unexpected HTTP response status.", statusCode, responseBody);
        }
    }
}