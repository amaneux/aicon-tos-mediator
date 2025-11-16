package com.aicon.tos.connect.http;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HttpConnectorTest {

    private static WireMockServer wireMockServer;

    @BeforeAll
    static void startWireMockServer() {
        wireMockServer = new WireMockServer(WireMockConfiguration.wireMockConfig().dynamicPort());
        wireMockServer.start();
    }

    @AfterAll
    static void stopWireMockServer() {
        if (wireMockServer != null) {
            wireMockServer.stop();
        }
    }

    @Test
    void testSendHttpRequestWithPost() {
        wireMockServer.stubFor(post(urlEqualTo("/api"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("POST ok")));

        ConfigGroup config = mock(ConfigGroup.class);
        String fullUrl = String.format("http://localhost:%d/api", wireMockServer.port());
        when(config.getItemValue(ConfigSettings.CFG_HTTP_METHOD, ConfigSettings.CFG_DEFAULT_METHOD)).thenReturn("POST");
        when(config.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn(fullUrl);

        HttpConnector connector = new HttpConnector();
        String result = connector.sendHttpRequest(config, "dummy");

        assertEquals("POST ok", result);
    }

    @Test
    void testSendHttpRequestWithGet() {
        wireMockServer.stubFor(get(urlEqualTo("/api"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("GET ok")));

        ConfigGroup config = mock(ConfigGroup.class);
        String fullUrl = String.format("http://localhost:%d/api", wireMockServer.port());
        when(config.getItemValue(ConfigSettings.CFG_HTTP_METHOD, ConfigSettings.CFG_DEFAULT_METHOD)).thenReturn("GET");
        when(config.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn(fullUrl);

        HttpConnector connector = new HttpConnector();
        String result = connector.sendHttpRequest(config, null);

        assertEquals("GET ok", result);
    }

    @Test
    void testSendHttpRequestWithUnsupportedMethod() {
        ConfigGroup config = mock(ConfigGroup.class);
        when(config.getItemValue(ConfigSettings.CFG_HTTP_METHOD, ConfigSettings.CFG_DEFAULT_METHOD)).thenReturn("PUT");
        when(config.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn("http://localhost:1234");

        HttpConnector connector = new HttpConnector();
        try {
            connector.sendHttpRequest(config, "test");
        } catch (UnsupportedOperationException e) {
            assertEquals("Unsupported HTTP method: PUT", e.getMessage());
        }
    }


    @Test
    void testSendHttpRequestPostWithoutBody() {
        ConfigGroup config = mock(ConfigGroup.class);
        when(config.getItemValue(ConfigSettings.CFG_HTTP_METHOD, ConfigSettings.CFG_DEFAULT_METHOD)).thenReturn("POST");
        when(config.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn("http://localhost:1234");

        HttpConnector connector = new HttpConnector();
        try {
            connector.sendHttpRequest(config, "");
        } catch (IllegalArgumentException e) {
            assertEquals("POST request requires a non-null and non-empty body.", e.getMessage());
        }
    }
}
