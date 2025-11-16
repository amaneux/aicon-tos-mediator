
package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.schema.AiconTosControlMessage;
import com.aicon.tos.shared.schema.OperatingMode;
import generated.AiconTosControlRequest;
import generated.AiconTosControlResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ControlFlowSession}.
 * This test verifies the transformation logic and execution behavior of the session.
 */
class ControlFlowSessionTest {

    private FlowController controller;
    private ConfigGroup kafkaConfig;
    private ConfigGroup flowConfig;
    private ConfigGroup httpConfig;
    private RequestResponseTransformer transformer;
    private AiconTosControlMessage controlMessage;
    private ConsumerRecord<String, AiconTosControlMessage> record;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        controller = mock(FlowController.class);
        kafkaConfig = mock(ConfigGroup.class);
        flowConfig = mock(ConfigGroup.class);
        httpConfig = mock(ConfigGroup.class);
        transformer = mock(RequestResponseTransformer.class);

        controlMessage = new AiconTosControlMessage();
        controlMessage.setId("CTRL-123");
        controlMessage.setOperatingMode(OperatingMode.ON);
        controlMessage.setIgnore(true);

        record = mock(ConsumerRecord.class);
        when(record.value()).thenReturn(controlMessage);
        when(controller.getNewControlRequestId()).thenReturn("REQ-456");
        when(flowConfig.getName()).thenReturn("ControlFlow");
    }

    @Test
    void testTransformMessageToRequest_shouldCreateValidRequest() {
        ControlFlowSession session = new ControlFlowSession(
                controller, record, kafkaConfig, flowConfig, httpConfig, transformer, "TOS");

        AiconTosControlRequest request = session.transformMessageToRequest(controlMessage);

        assertNotNull(request);
        assertEquals("REQ-456", request.getRequestId());
        assertNotNull(request.getRequestTs());
        assertEquals(1, request.getPropertyGroup().size());

        List<AiconTosControlRequest.PropertyGroup.Property> props =
                request.getPropertyGroup().get(0).getProperty();
        assertEquals(3, props.size());
        assertEquals("STATE", props.get(0).getKey());
        assertEquals("ON", props.get(0).getValue());
    }

    @Test
    void testTransformResponseToMessage_shouldCreateCorrectControlMessage() {
        AiconTosControlResponse response = new AiconTosControlResponse();
        response.setRequestId("RESP-123");

        ControlFlowSession session = new ControlFlowSession(
                controller, record, kafkaConfig, flowConfig, httpConfig, transformer, "TOS");

        AiconTosControlMessage result = session.transformResponseToMessage(response);
        assertEquals("RESP-123", result.getId());
        assertEquals("none", result.getComment());
    }

    @Test
    void testExecute_shouldPerformFullFlow() {
        ControlFlowSession session = new ControlFlowSession(
                controller, record, kafkaConfig, flowConfig, httpConfig, transformer, "TOS") {

            @Override
            protected String sendHttpRequest(Object request, Class<?> clazz) {
                assertInstanceOf(AiconTosControlRequest.class, request);
                return "<mocked-response/>";
            }

            @Override
            protected <T> T deserializeResponse(String rawResponse, Class<T> clazz) {
                assertEquals("<mocked-response/>", rawResponse);
                AiconTosControlResponse response = new AiconTosControlResponse();
                response.setRequestId("CTRL-123");
                return clazz.cast(response);
            }
        };

        assertDoesNotThrow(session::execute);
    }
}
