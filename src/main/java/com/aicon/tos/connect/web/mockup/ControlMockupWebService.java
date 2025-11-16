package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.shared.exceptions.DeserializationException;
import com.aicon.tos.shared.exceptions.SerializationException;
import generated.AiconTosControlRequest;
import generated.AiconTosControlResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.stream.Collectors;

import static com.aicon.tos.shared.xml.XmlUtil.deserialize;
import static com.aicon.tos.shared.xml.XmlUtil.serialize;

public class ControlMockupWebService extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ControlMockupWebService.class);
    private static final List<WebServiceListener> listeners = new ArrayList<>();
    private static volatile AiconTosControlResponse modifiedResponse;
    private static boolean blockresponseSend = false;

    public static void registerListener(WebServiceListener listener) {
        listeners.add(listener);
    }

    public static void unregisterListener(WebServiceListener listener) {
        listeners.remove(listener);
    }

    public static void setBlockResponseSend(boolean block) {
        blockresponseSend = block;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        LOG.info(">>>>>>>> POST request received at /controlMockupWebService");

        try {
            // Read and log the request body
            String requestBody = req.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
            LOG.info("Request Body: {}", requestBody);

            // Deserialize the request
            AiconTosControlRequest request = deserialize(LOG, requestBody, AiconTosControlRequest.class);

            // Notify listeners
            for (WebServiceListener listener : listeners) {
                listener.onRequestReceived(request);
            }

            // Create or use the modified response
            if (modifiedResponse == null) {
                modifiedResponse = createResponse(request);
            }

            // Block until response is ready
            synchronized (this) {
                while (blockresponseSend) {
                    this.wait(1000);
                }
            }

            // Serialize and send the response
            sendResponse(resp, modifiedResponse);

        }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            handleException(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Request processing was interrupted", e);
        }
        catch (DeserializationException e) {
            LOG.error ("Deserialization request error", e);
        }
    }


    public static AiconTosControlResponse createResponse(AiconTosControlRequest request) {
        AiconTosControlResponse response = new AiconTosControlResponse();
        response.setRequestId(request.getRequestId());

        long currentMillis = Instant.now().toEpochMilli();
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(currentMillis);
        return response;
    }

    private void sendResponse(HttpServletResponse resp, Object response) throws IOException {
        try {
            String xmlResponse = serialize(response, AiconTosControlRequest.class);
            resp.setContentType("application/xml");
            resp.setCharacterEncoding("UTF-8");
            resp.setStatus(HttpServletResponse.SC_OK);

            try (PrintWriter out = resp.getWriter()) {
                out.write(xmlResponse);
            }
        }
        catch (SerializationException e) {
            LOG.error("Error serializing response to XML", e);
        }
    }

    private void handleException(HttpServletResponse resp, int statusCode, String errorMessage, Exception e)
            throws IOException {
        LOG.error(errorMessage, e);
        resp.setStatus(statusCode);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(String.format("{\"error\": \"%s\"}", errorMessage));
    }

    public interface WebServiceListener {
        void onRequestReceived(AiconTosControlRequest requestBody);
    }
}
