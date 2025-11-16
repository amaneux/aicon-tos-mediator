package com.aicon.tos.connect.http;

import com.aicon.tos.connect.web.mockup.TestJSONServlet;
import com.aicon.tos.connect.web.mockup.TestXMLServlet;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.apache.catalina.Context;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.startup.Tomcat;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.aicon.tos.shared.config.ConfigType.CONTENT_TYPE_JSON;
import static com.aicon.tos.shared.config.ConfigType.CONTENT_TYPE_XML;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HttpConnectorIT {

    private static final Logger LOG = LoggerFactory.getLogger(HttpConnectorIT.class);

    private static Tomcat tomcat;

    private static final String DEFAULT_METHOD = "POST";
    private static final String DEFAULT_CONN_TO = "5000";
    private static final String DEFAULT_READ_TO = "10000";
    private static final String DEFAULT_CONTENT_TYPE = CONTENT_TYPE_XML;


    @BeforeAll
    public static void startTomcat() throws LifecycleException, IOException {
        // Start the embedded Tomcat server
        tomcat = new Tomcat();
        tomcat.setPort(8888);
        tomcat.getConnector();

        // Create a context for the servlet
        Context ctx = tomcat.addContext("", new File(".").getAbsolutePath());

        // Add the TestXMLServlet to Tomcat via the context
        Tomcat.addServlet(ctx, "TestXMLServlet", new TestXMLServlet());
        ctx.addServletMappingDecoded("/testXMLServlet", "TestXMLServlet");

        // Add the TestJSONServlet to Tomcat via the context
        Tomcat.addServlet(ctx, "TestJSONServlet", new TestJSONServlet());
        ctx.addServletMappingDecoded("/testJSONServlet", "TestJSONServlet");

        // Start the server
        tomcat.start();

        // Check if servlets are registered
        if (isServletRegistered("TestXMLServlet")) {
            throw new IllegalStateException("TestXMLServlet is not registered!");
        }

        if (isServletRegistered("TestJSONServlet")) {
            throw new IllegalStateException("TestJSONServlet is not registered!");
        }

        // Make a test GET request to each servlet
        int xmlResponseCode = sendGetRequest("http://localhost:8888/testXMLServlet");
        if (xmlResponseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException(
                    "TestXMLServlet did not respond correctly! Response code: " + xmlResponseCode);
        }

        int jsonResponseCode = sendGetRequest("http://localhost:8888/testJSONServlet");
        if (jsonResponseCode != HttpURLConnection.HTTP_OK) {
            throw new IllegalStateException(
                    "TestJSONServlet did not respond correctly! Response code: " + jsonResponseCode);
        }

        LOG.info("TestXMLServlet is registered and responding on /testXMLServlet");
        LOG.info("TestJSONServlet is registered and responding on /testJSONServlet");
    }


    public static boolean isServletRegistered(String servletName) {
        Context ctx = (Context) tomcat.getHost().findChild("");
        return ctx.findChild(servletName) == null;
    }

    private static int sendGetRequest(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setRequestMethod("GET");
        connection.connect();
        return connection.getResponseCode();
    }

    @AfterAll
    public static void stopTomcat() throws LifecycleException {
        // Stop the Tomcat server after all tests
        if (tomcat != null) {
            tomcat.stop();
            tomcat.destroy();
        }
    }

    @Test
    void testSendJSONHttpRequest() throws Exception {
        // Create a mock ConfigGroup
        ConfigGroup mockConfigGroup = mock(ConfigGroup.class);

        // Define the behavior of getItems to return the modifiable list
        when(mockConfigGroup.isOfType(ConfigType.Http)).thenReturn(true);
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn(
                "http://localhost:8888/testJSONServlet");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_METHOD, DEFAULT_METHOD)).thenReturn("POST");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_CONTENT_TYPE, DEFAULT_CONTENT_TYPE)).thenReturn(
                CONTENT_TYPE_JSON);
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_CONNECT_TIMEOUT_MS, DEFAULT_CONN_TO)).thenReturn(
                "5000");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_READ_TIMEOUT_MS, DEFAULT_READ_TO)).thenReturn(
                "10000");

        // Instantiate the HttpConnector
        HttpConnector httpConnector = new HttpConnector();

        // Send a test message to the TestXMLServlet
        String requestPayload = "Test message to testJSONServlet";
        String response = httpConnector.sendHttpRequest(mockConfigGroup, requestPayload);

        // Validate the expected JSON response
        String expectedJSONResponse = """
                {
                  "APIIdentifier": 12,\s
                  "requestIdx": "654321",
                  "timestamp": 1234567890,
                  "errCode": 200,
                  "errMsg": ""
                }
                """;

        System.out.println("Actual Response: " + response);
        System.out.println("Expected Response: " + expectedJSONResponse);

        ObjectMapper mapper = new ObjectMapper();
        assertEquals(mapper.readTree(expectedJSONResponse), mapper.readTree(response)); // Ignores whitespace
    }

    @Test
    void testSendXMLHttpRequest() throws Exception {
        // Create a mock ConfigGroup
        ConfigGroup mockConfigGroup = mock(ConfigGroup.class);

        // Define the behavior of getItems to return the modifiable list
        when(mockConfigGroup.isOfType(ConfigType.Http)).thenReturn(true);
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_URL, "")).thenReturn(
                "http://localhost:8888/testXMLServlet");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_METHOD, DEFAULT_METHOD)).thenReturn("POST");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_CONTENT_TYPE, DEFAULT_CONTENT_TYPE)).thenReturn(
                CONTENT_TYPE_XML);
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_CONNECT_TIMEOUT_MS, DEFAULT_CONN_TO)).thenReturn(
                "5000");
        when(mockConfigGroup.getItemValue(ConfigSettings.CFG_HTTP_READ_TIMEOUT_MS, DEFAULT_READ_TO)).thenReturn(
                "10000");

        // Instantiate the HttpConnector
        HttpConnector httpConnector = new HttpConnector();

        // Send a test message to the TestXMLServlet
        String requestPayload = "Test message to testXMLServlet";
        String response = httpConnector.sendHttpRequest(mockConfigGroup, requestPayload);

        // Validate the expected XML response
        String expectedXMLResponse =
                """
                        <APIResponse>
                            <APIIdentifier>12</APIIdentifier>
                            <requestIdx>654321</requestIdx>
                            <timestamp>1234567890</timestamp>
                            <errCode>200</errCode>
                            <errMsg></errMsg>
                        </APIResponse>
                        """;

        System.out.println("Actual Response: " + response);
        System.out.println("Expected Response: " + expectedXMLResponse);

        // Compare XML with Jackson's XmlMapper

        System.out.println("Jackson Version: " +
                com.fasterxml.jackson.core.JsonGenerator.Feature.class.getPackage().getImplementationVersion());
        XmlMapper xmlMapper = new XmlMapper();
        assertEquals(
                xmlMapper.readTree(expectedXMLResponse),
                xmlMapper.readTree(response)
        );
    }
}
