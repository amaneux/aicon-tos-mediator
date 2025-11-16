package com.aicon.tos.connect.web.mockup;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class TestBasicJSONWebService extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(TestBasicJSONWebService.class);


    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        LOG.info("TestBasicJSONWebService doGet");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.info(">>>>>>>> POST request received at /basicJSONWebService");
    }
}
