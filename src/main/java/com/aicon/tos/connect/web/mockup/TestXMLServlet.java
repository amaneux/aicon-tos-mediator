package com.aicon.tos.connect.web.mockup;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;

public class TestXMLServlet extends HttpServlet {

    protected static final Logger LOG = LoggerFactory.getLogger(TestXMLServlet.class);

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
                <APIResponse>
                    <APIIdentifier>12</APIIdentifier>
                    <requestIdx>654321</requestIdx>
                    <timestamp>1234567890</timestamp>
                    <errCode>200</errCode>
                    <errMsg></errMsg>
                </APIResponse>
                """;

        // Write response
        PrintWriter out = response.getWriter();
        out.println(content);
    }
}
