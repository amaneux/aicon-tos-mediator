package com.aicon.tos.connect.http;

import com.aicon.tos.exceptions.HttpException;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

/**
 * HttpConnector provides a wrapper around Java's HttpClient for making
 * HTTP requests, delegating response handling to the HttpResponseHandler.
 */
public class HttpConnector {

    private static final Logger LOG = LoggerFactory.getLogger(HttpConnector.class);
    private final HttpClient client;

    /**
     * Constructor to initialize the HttpConnector with a custom HttpClient configuration.
     */
    public HttpConnector() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();
    }

    /**
     * Sends an HTTP GET request to the specified URI and processes the response via HttpResponseHandler.
     *
     * @param uri      The target URI.
     * @param headers  Optional headers to include in the request.
     * @return The response body as a String.
     */
    public String sendGet(String uri, Map<String, String> headers) {
        LOG.info("Sending GET request to URI: {}", uri);

        // Build the HTTP GET request.
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                .uri(URI.create(uri))
                .GET();

        // Add headers if provided.
        if (headers != null) {
            headers.forEach(requestBuilder::header);
        }
        HttpRequest request = requestBuilder.build();

        // Send the request and delegate response processing to HttpResponseHandler.
        HttpResponse<String> response = null;
        try {
            response = client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOG.error("An error occurred during the HTTP GET request to URI: {}", uri, e);
            throw new HttpException("Error during HTTP GET request to URI: " + uri, -1, e.getMessage());
        }
        return HttpResponseHandler.handleHttpResponse(response);

    }

    /**
     * Sends an HTTP POST request with a body and processes the response via HttpResponseHandler.
     *
     * @param uri      The target URI.
     * @param headers  Optional headers to include in the request.
     * @param body     The request body to send.
     * @return The response body as a String.
     */
    public String sendPost(String uri, Map<String, String> headers, String body) {
        try {
            LOG.info("Sending POST request to URI: {}", uri);

            // Build the HTTP POST request.
            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(uri))
                    .POST(HttpRequest.BodyPublishers.ofString(body));

            // Add headers if provided.
            if (headers != null) {
                headers.forEach(requestBuilder::header);
            }
            HttpRequest request = requestBuilder.build();

            // Send the request and delegate response processing to HttpResponseHandler.
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return HttpResponseHandler.handleHttpResponse(response);

        } catch (IOException | InterruptedException e) {
            LOG.error("An error occurred during the HTTP POST request to URI: {}", uri, e);
            throw new HttpException("Error during HTTP POST request to URI: " + uri, -1, e.getMessage());
        }
    }

    /**
     * Sends an HTTP request (GET or POST) based on the provided configuration and path.
     *
     * @param httpConfig  The HTTP configuration for the request.
     * @param requestBody The body of the request (for POST). Can be null or empty for GET.
     * @return The response as a String.
     * @throws UnsupportedOperationException If the HTTP method is invalid or unsupported.
     */
    public String sendHttpRequest(ConfigGroup httpConfig, String requestBody) {
        // Extract the HTTP method from configuration
        String httpMethod = httpConfig.getItemValue(ConfigSettings.CFG_HTTP_METHOD, ConfigSettings.CFG_DEFAULT_METHOD);
        String url = httpConfig.getItemValue(ConfigSettings.CFG_HTTP_URL, "");

        if (url == null || url.isEmpty()) {
            throw new IllegalArgumentException("URL must be provided in HTTP configuration.");
        }

        Map<String, String> headers = buildDefaultHeaders(httpConfig);

        // Execute appropriate HTTP method
        if ("GET".equalsIgnoreCase(httpMethod)) {
            return sendGet(url, headers);
        } else if ("POST".equalsIgnoreCase(httpMethod)) {
            if (requestBody == null || requestBody.isEmpty()) {
                throw new IllegalArgumentException("POST request requires a non-null and non-empty body.");
            }
            return sendPost(url, headers, requestBody);
        } else {
            // Reject unsupported methods
            throw new UnsupportedOperationException("Unsupported HTTP method: " + httpMethod);
        }
    }

    /**
     * Builds default HTTP headers for requests.
     *
     * @return A map of default HTTP headers.
     */
    public Map<String, String> buildDefaultHeaders(ConfigGroup httpConfig) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Accept", "application/json");
//        headers.put("SOAPAction", httpConfig.getItemValue(ConfigSettings.CFG_HTTP_SOAP_ACTION, ""));
        headers.put("SOAPAction","");
        String credentials = httpConfig.getItemValue(ConfigSettings.CFG_HTTP_USERNAME) + ":" +
                httpConfig.getItemValue(ConfigSettings.CFG_HTTP_PASSWORD);
        String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes());
        headers.put("Authorization", "Basic " + encodedCredentials);
        return headers;
    }
}