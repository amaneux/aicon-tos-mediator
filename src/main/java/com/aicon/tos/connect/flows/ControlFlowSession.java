package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.schema.AiconTosControlMessage;
import generated.AiconTosControlRequest;
import generated.AiconTosControlResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.aicon.tos.shared.util.GregorianCalendarUtil.getNowInXMLGregorianCalender;

/**
 * Represents a specialization of {@link HttpFlowSession} designed for handling control flow-related operations.
 * This class facilitates the transformation and handling of control messages into HTTP requests while managing
 * configurations for specific control flow use cases.
 * <ul>
 *     <li>Receives a control message, processes its content, and converts it into a request to be sent via HTTP.</li>
 *     <li>Logs detailed session and processing steps for observability.</li>
 *     <li>Handles exceptions by encapsulating them within a custom {@link FlowSessionException}.</li>
 * </ul>
 */
public class ControlFlowSession extends HttpFlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(ControlFlowSession.class);
    private final AiconTosControlMessage controlMessage;

    public ControlFlowSession(FlowController flowController,
                              ConsumerRecord<String, AiconTosControlMessage> controlMessageRecord,
                              ConfigGroup kafkaConfig, ConfigGroup flowConfig, ConfigGroup httpConfig,
                              RequestResponseTransformer transformer, String n4Scope) {
        super(flowController, "CONTROL_FLOW", httpConfig, flowConfig, kafkaConfig, transformer, n4Scope);
        this.controlMessage = controlMessageRecord.value();
        LOG.info("Instantiate Control FLOW session");
    }

    @Override
    public void execute() throws FlowSessionException {
        LOG.info("Starting Control FLOW session");

        LOG.info("controlMessage: Id={}, OperatingMode={}, TimeSync={} Ignore={}",
                controlMessage.getId(),
                controlMessage.getOperatingMode(),
                controlMessage.getTimeSync(),
                controlMessage.getIgnore());

        AiconTosControlRequest request = transformMessageToRequest(controlMessage);

        try {
            String rawResponse = sendHttpRequest(request, AiconTosControlRequest.class);
            AiconTosControlResponse controlResponse = deserializeResponse(rawResponse, AiconTosControlResponse.class);

            // Optional: handle response content
            LOG.info("Control FLOW response received with requestId = {}", controlResponse.getRequestId());
        } catch (Exception e) {
            throw new FlowSessionException("ControlFlowSession execution failed", e);
        }
    }

    protected AiconTosControlRequest transformMessageToRequest(AiconTosControlMessage message) {
        AiconTosControlRequest request = new AiconTosControlRequest();
        request.setRequestId(controller.getNewControlRequestId());
        request.setRequestTs(getNowInXMLGregorianCalender());
        request.setIgnore(message.getIgnore());

        AiconTosControlRequest.PropertyGroup group = new AiconTosControlRequest.PropertyGroup();
        group.setGroupName("AICON CONTROL");

        group.getProperty().addAll(List.of(
                createProperty("STATE", message.getOperatingMode().toString()),
                createProperty("LOG_EVENT", "ON"),
                createProperty("AICON_USER", "-notices-")
        ));

        request.getPropertyGroup().add(group);
        return request;
    }

    protected AiconTosControlMessage transformResponseToMessage(AiconTosControlResponse response) {
        AiconTosControlMessage tosControlMessage = new AiconTosControlMessage();
        tosControlMessage.setId(response.getRequestId());
        tosControlMessage.setComment("none");
        return tosControlMessage;
    }

        private AiconTosControlRequest.PropertyGroup.Property createProperty(String key, String value) {
        AiconTosControlRequest.PropertyGroup.Property property = new AiconTosControlRequest.PropertyGroup.Property();
        property.setKey(key);
        property.setValue(value);
        return property;
    }

    @Override
    protected String getThreadName(int threadCounter) {
        return String.format("%s-%s", flowConfig.getName(), threadCounter);
    }
}
