package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.http.HttpConnector;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.exceptions.DeserializationException;
import com.aicon.tos.shared.exceptions.SerializationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.aicon.tos.shared.xml.XmlUtil.deserialize;
import static com.aicon.tos.shared.xml.XmlUtil.prettyPrintXml;
import static com.aicon.tos.shared.xml.XmlUtil.serialize;

/**
 * Abstract base class for all HTTP-based FlowSessions.
 * <p>
 * This class provides:
 * - Request/response transformation via {@link RequestResponseTransformer}
 * - Scoped transformations using N4 scope
 * - Sending and receiving HTTP requests via {@link HttpConnector}
 * <p>
 * Subclasses implement {@link #execute()} for flow-specific logic.
 */
public abstract class HttpFlowSession extends FlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(HttpFlowSession.class);

    protected final RequestResponseTransformer transformer;
    protected final String n4Scope;
    protected final HttpConnector connector;

    protected HttpFlowSession(FlowController controller, String sessionType,
                              ConfigGroup httpConfig, ConfigGroup flowConfig, ConfigGroup kafkaConfig,
                              RequestResponseTransformer transformer, String n4Scope) {
        super(controller, sessionType, httpConfig, flowConfig, kafkaConfig);
        this.transformer = transformer;
        this.n4Scope = n4Scope;
        this.connector = new HttpConnector(); // Delegates HTTP logic
    }

    /**
     * Serialize and transform the request object and send it to the endpoint.
     */
    protected String sendHttpRequest(Object request, Class<?> requestClass) {
        // Serialize the request object
        String serialized = null;
        try {
            serialized = serialize(request, requestClass);
        } catch (SerializationException e) {
            LOG.error("Serialization error, %s", e);
            return null;
        }

        String transformedRequest;
        // Transform the serialized request using the configured transformer
        if (transformer!=null) {
            transformedRequest = transformer.transformRequest(serialized, n4Scope);
            LOG.info("Transformed {} Request: {}", getSessionType(), transformedRequest);
        } else {
            transformedRequest = serialized;
            LOG.info("UNTransformed {} Request: {}", getSessionType(), transformedRequest);
        }
        // Send as a POST request by default
        String response = connector.sendHttpRequest(httpConfig, transformedRequest);
        if (LOG.isTraceEnabled()) {
            LOG.trace("Raw Response: \n{}", prettyPrintXml(response));
        }

        //TODO:
//        ERROR_UNSUPPORTED_XML_ROOT means that no groovy is installed that understands this msg type;
//        Parse the response correct. There can also be errors....
//                <soapenv:Body>
//                <basicInvokeResponse xmlns="http://www.navis.com/services/argobasicservice">
//                <basicInvokeResponse>
//                <argo-response status="3" status-id="SEVERE">
//                <messages>
//                <message message-id="ERROR_UNSUPPORTED_XML_ROOT" message-severity="SEVERE" message-text="Webservice support doesnt exist for root {0}." message-detail="Webservice support doesnt exist for root {0}."/>
//                </messages>
//                <argo:custom-response xmlns:argo="http://www.navis.com/argo">
//                <aicon-tos-error-response resultLevel="FATAL" resultAudience="SYSTEM" resultCode="UNKNOWN_MESSAGE" resultText="com.avlino.aicon.yms.n4integration.AlenzaYardsightWebserviceListener$_execute_closure1@5dbd6908"/>
//                </argo:custom-response>
//                </argo-response>
//                </basicInvokeResponse>
//                </basicInvokeResponse>
//                </soapenv:Body>
//                </soapenv:Envelope>
//
//                And since we are now sending an unknown request for Navis you will get one general aicon-tos-error-response
//                   back stating that it was an unknown request
//          Preferably this is show somewhere in the UI
        return response;
    }

    /**
     * Transform and deserialize the HTTP response.
     */
    protected <T> T deserializeResponse(String rawResponse, Class<T> responseClass) throws FlowSessionException {
        String transformed = transformer.transformResponse(rawResponse);
        LOG.info("Transformed {} Response: {}", getSessionType(), transformed);
        try {
            return deserialize(LOG, transformed, responseClass);
        } catch (DeserializationException e) {
            LOG.error("Failed to deserialize response for session {}: {}", getSessionType(), e.getMessage(), e);
            throw new FlowSessionException("Deserialization failed for session: " + getSessionType(), e);
        }

    }

    /**
     * Subclasses must implement the execute method to provide flow-specific logic.
     */
    @Override
    public abstract void execute() throws FlowSessionException;
}