package com.aicon.tos.connect.web.mockup;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class TestJSONServlet extends HttpServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(TestJSONServlet.class);

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        // Read data from the request
        StringBuilder reqBuffer = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                reqBuffer.append(line);
            }
        }
        String content = reqBuffer.toString();
        LOG.info("Received request: \n{}", content);

        // Set response content type
        response.setContentType(request.getContentType());

        content = """
                {
                  "APIIdentifier": 12,
                  "requestIdx": "654321",
                  "timestamp": 1234567890,
                  "errCode": 200,
                  "errMsg": ""
                }
                """;
        // Example response from PNC
        // {"APIIdentifier":null,"requestIdx":"SWAP_INDEX_65432164","timestamp":1730280125809,"errCode":900,"errMsg":"[SWAP-LOAD] There is no requests list"}

        // Write response
        PrintWriter out = response.getWriter();
        out.println(content);
    }
}
