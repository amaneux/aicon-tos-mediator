
package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.cdc.CDCConfig;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import com.aicon.tos.connect.http.HttpConnector;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusProducer;
import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;
import com.aicon.tos.shared.schema.ConnectionStatus;
import generated.AiconTosCanaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CanarySessionTest {

    private CanaryController mockCanaryController;
    private CDCController mockCDCController;
    private CDCDataProcessor mockProcessor;
    private RequestResponseTransformer mockTransformer;
    private ConfigGroup mockHttpConfig;
    private AiconTosConnectionStatusProducer mockStatusProducer;

    @BeforeEach
    void setup() {
        mockCanaryController = mock(CanaryController.class);
        mockCDCController = mock(CDCController.class);
        mockProcessor = mock(CDCDataProcessor.class);
        mockTransformer = mock(RequestResponseTransformer.class);
        mockHttpConfig = mock(ConfigGroup.class);
        mockStatusProducer = mock(AiconTosConnectionStatusProducer.class);

        when(mockCanaryController.getCDCController()).thenReturn(mockCDCController);
    }

    @Test
    void testExecute_withCdcCheck_sendsCorrectStatusMessage() {
        when(mockTransformer.transformRequest(any(), eq("TOS"))).thenReturn("<transformed-request/>");
        when(mockTransformer.transformResponse(any())).thenReturn("<transformed-response/>");

        when(mockProcessor.getCDCTableNames()).thenReturn("TABLE1,TABLE2");
        when(mockProcessor.convertRawStringToPrettyCDCString(anyString())).thenReturn("pretty-CDC");
        when(mockProcessor.expectedTOSCDCDataForConfiguredTables(anyString())).thenReturn(true);
        when(mockProcessor.removeTooOldData(anyString())).thenReturn("filtered-CDC");
        when(mockProcessor.removeTooOldData(anyList())).thenReturn(List.of());
        when(mockProcessor.convertStringToCDCData(anyString())).thenReturn(List.of());
        when(mockProcessor.getCdcConfigForTable(anyString())).thenReturn(mock(CDCConfig.class));
        when(mockProcessor.determineCdcStatus(anyString(), anyString())).thenReturn(true);

        CanarySession session = new CanarySession(
                mockCanaryController,
                mockProcessor,
                mockStatusProducer,
                mockHttpConfig,
                mockTransformer,
                "TOS",
                true
        ) {
            @Override
            protected HttpConnector getHttpConnector() {
                return new HttpConnector() {
                    @Override
                    public String sendHttpRequest(ConfigGroup config, String request) {
                        return "<raw-response/>";
                    }
                };
            }
            @Override
            protected AiconTosCanaryResponse deserializeResponse(String transformedResponse) {
                AiconTosCanaryResponse response = new AiconTosCanaryResponse();
                response.setRequestId("12345");
                response.setRequestSendTsMillis(100L);
                response.setRequestReceivedTsMillis(200L);
                response.setResponseSendTsMillis(300L);
                response.setResponseReceivedTsMillis(400L);
                response.setLatestCdcCreations("cdc-raw-data");
                return response;
            }

            @Override
            protected void sendConnectionStatusMessageIfAvailable() {
                AiconTosConnectionStatusMessage msg = new AiconTosConnectionStatusMessage();
                msg.setRequestId("12345");
                msg.setConnectionStatus(ConnectionStatus.OK);
                msg.setCdcOk(Boolean.TRUE);
                mockStatusProducer.sendMessage("12345", msg);
            }

        };

        // Act
        session.run();

        // Assert
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<AiconTosConnectionStatusMessage> msgCaptor = ArgumentCaptor.forClass(AiconTosConnectionStatusMessage.class);

        verify(mockStatusProducer).sendMessage(keyCaptor.capture(), msgCaptor.capture());

        assertEquals("12345", keyCaptor.getValue());
        assertEquals(ConnectionStatus.OK, msgCaptor.getValue().getConnectionStatus());
        assertEquals(Boolean.TRUE, msgCaptor.getValue().getCdcOk());
    }
}
