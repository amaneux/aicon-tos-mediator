package com.aicon.tos.connect.flows;

import com.aicon.TestConstants;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestHelper;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateResponseMessage;
import com.aicon.tos.shared.schema.DeckMove;
import com.aicon.tos.shared.schema.Result;
import com.aicon.tos.shared.util.GregorianCalendarUtil;
import generated.AiconYardDeckingUpdateRequest;
import generated.AiconYardDeckingUpdateResponse;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static com.aicon.tos.shared.util.TimeUtils.convertToXMLGregorianCalendar;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the DeckingUpdateFlowSession class, specifically targeting the handling of TOS Decking
 * Update requests and responses by verifying the transformation and deserialization processes.
 * The test class ensures the interactions with dependent components such as FlowController, ConfigGroup,
 * and RequestResponseTransformer are functioning as expected.
 */
class DeckingUpdateFlowSessionTest {

    private final String ID = "123";
    private final String WIG_KEY = "WIG-001";
    private final String POSITION_TO = "P1";
    private static final String TEST_FILE = TestConstants.TEST_CONFIG_FULL_FILESPEC;

    private FlowController controller;
    private ConfigGroup httpConfig;
    private ConfigGroup flowConfig;
    private RequestResponseTransformer transformer;
    private AiconYardDeckingUpdateRequestMessage message;
    private ConsumerRecord<String, AiconYardDeckingUpdateRequestMessage> consumerRecord;

    @BeforeEach
    void setUp() {
        ConfigSettings.setConfigFile(TEST_FILE);
        ConfigSettings config = ConfigSettings.getInstance();

        controller = mock(FlowController.class);
        httpConfig = mock(ConfigGroup.class);
        flowConfig = mock(ConfigGroup.class);
        transformer = mock(RequestResponseTransformer.class);

        message = new AiconYardDeckingUpdateRequestMessage();
        message.setRequestId(ID);
        message.setTimeStamp(System.currentTimeMillis());
        message.setTimeStampLocalTimeISO8601(Instant.now());

        DeckMove move = new DeckMove();
        move.setWiGkey(WIG_KEY);
        move.setPositionTo(POSITION_TO);
        move.setResult(new Result());
        move.getResult().setResultCode("200");
        message.setMoves(List.of(move));

        consumerRecord = mock(ConsumerRecord.class);
        when(consumerRecord.value()).thenReturn(message);
    }

    @Test
    void testTransformationInitializesEmptyMoves() {
        // Initialize the session with mocked dependencies
        DeckingUpdateFlowSession session = new DeckingUpdateFlowSession(
                controller,
                consumerRecord,
                flowConfig,
                httpConfig,
                transformer,
                "TOS"
        );

        AiconYardDeckingUpdateResponse response = new AiconYardDeckingUpdateResponse();
        response.setRequestId("TEST-ID");
        response.setResultCode("1");
        response.setResultText("Test Result");
        response.setTimeStamp(System.currentTimeMillis());
        response.setTimeStampLocalTimeISO8601(GregorianCalendarUtil.getNowInXMLGregorianCalender());

        AiconYardDeckingUpdateResponseMessage message = session.transformResponseToMessage(response);

        // Assertions to verify the response
        assertNotNull(message.getMoves(), "The 'moves' list should not be null");
        assertTrue(message.getMoves().isEmpty(), "The 'moves' list should be empty if no moves are provided");
    }


    @Test
    void shouldTransformRequestAndResponseAndDeserializeCorrectly() throws Exception {
        AiconYardDeckingUpdateRequest mockedRequest = AiconYardDeckingUpdateRequestHelper.createWithDefaults();
        mockedRequest.setRequestId(ID);
        // Ensure the response's moves list is always initialized
        mockedRequest.setMoves(new AiconYardDeckingUpdateRequest.Moves());


        AiconYardDeckingUpdateResponseMessage mockedResponseMsg = new AiconYardDeckingUpdateResponseMessage();
        // Set the required fields
        mockedResponseMsg.setRequestId(ID); // Ensure requestId is not null
        mockedResponseMsg.setTimeStamp(Instant.now().toEpochMilli());
        mockedResponseMsg.setTimeStampLocalTimeISO8601(Instant.now());

        // Ensure the response's moves list is always initialized
        mockedResponseMsg.setMoves(new ArrayList<>());

        DeckingUpdateFlowSession session = spy(new DeckingUpdateFlowSession(
                controller,
                consumerRecord,
                flowConfig,
                httpConfig,
                transformer,
                "TOS"
        ) {
            @Override
            protected String sendHttpRequest(Object request, Class<?> clazz) {
                assertEquals(ID, ((AiconYardDeckingUpdateRequest) request).getRequestId());
                return "<mocked-raw-response/>";
            }

            @Override
            protected <T> T deserializeResponse(String rawResponse, Class<T> clazz) {
                assertEquals("<mocked-raw-response/>", rawResponse);
                AiconYardDeckingUpdateResponse response = new AiconYardDeckingUpdateResponse();
                response.setRequestId(ID);
                return clazz.cast(response);
            }
        });

        when(flowConfig.getName()).thenReturn("TestFlow");
        doReturn(mockedRequest).when(session).transformMessageToRequest(message);
        doReturn(mockedResponseMsg).when(session).transformResponseToMessage(any());

        session.execute();

        verify(session).transformMessageToRequest(message);
        verify(session).transformResponseToMessage(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformMessageToRequest() {
        AiconYardDeckingUpdateRequestMessage mockMsg = new AiconYardDeckingUpdateRequestMessage();
        mockMsg.setRequestId(ID);
        DeckMove mockMove = new DeckMove();
        mockMove.setResult(new Result());
        mockMove.setWiGkey(WIG_KEY);
        mockMove.setPositionTo(POSITION_TO);
        mockMove.getResult().setResultCode("200");
        mockMove.getResult().setResultText("Operation Successful");
        mockMsg.setMoves(List.of(mockMove));
        mockMsg.setTimeStamp(System.currentTimeMillis());
        mockMsg.setTimeStampLocalTimeISO8601(Instant.now());
        ConsumerRecord<String, AiconYardDeckingUpdateRequestMessage> rec = mock(ConsumerRecord.class);
        when(rec.value()).thenReturn(mockMsg);

        DeckingUpdateFlowSession session = new DeckingUpdateFlowSession(
                controller, rec, flowConfig, httpConfig, transformer, "TOS");

        AiconYardDeckingUpdateRequest request = session.transformMessageToRequest(mockMsg);

        assertEquals(ID, request.getRequestId());
        assertNotNull(request.getMoves());
        assertEquals(1, request.getMoves().getDeckMove().size());
        assertEquals(WIG_KEY, request.getMoves().getDeckMove().get(0).getWiGkey());
        assertEquals(POSITION_TO, request.getMoves().getDeckMove().get(0).getPositionTo());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTransformResponseToMessage() throws Exception {
        // Prepare a mock response with required fields
        AiconYardDeckingUpdateResponse mockResponse = new AiconYardDeckingUpdateResponse();
        mockResponse.setRequestId(ID);
        mockResponse.setTimeStamp(System.currentTimeMillis());
        mockResponse.setTimeStampLocalTimeISO8601(GregorianCalendarUtil.getNowInXMLGregorianCalender());

        mockResponse.setResultLevel("INFO");
        mockResponse.setResultAudience("Test Audience");
        mockResponse.setResultCode("200");
        mockResponse.setResultText("Operation Successful");

        AiconYardDeckingUpdateResponse.Moves moves = new AiconYardDeckingUpdateResponse.Moves();
        AiconYardDeckingUpdateResponse.Moves.DeckMove deckMove = new AiconYardDeckingUpdateResponse.Moves.DeckMove();
        deckMove.setWiGkey(WIG_KEY);
        deckMove.setResultAudience("Move Audience");
        deckMove.setResultCode("123");
        deckMove.setResultLevel("DEBUG");
        deckMove.setResultText("Move Successful");
        moves.getDeckMove().add(deckMove);
        mockResponse.setMoves(moves);

        // Mock the consumer record and session
        ConsumerRecord<String, AiconYardDeckingUpdateRequestMessage> rec = mock(ConsumerRecord.class);
        when(rec.value()).thenReturn(new AiconYardDeckingUpdateRequestMessage());

        DeckingUpdateFlowSession session = new DeckingUpdateFlowSession(
                controller, rec, flowConfig, httpConfig, transformer, "TOS");

        // Transform the mock response into a message
        AiconYardDeckingUpdateResponseMessage responseMessage = session.transformResponseToMessage(mockResponse);

        // Verify the transformed fields
        assertNotNull(responseMessage, "Response message should not be null");
        assertEquals(ID, responseMessage.getRequestId());
        assertEquals(mockResponse.getTimeStamp(), responseMessage.getTimeStamp());
        assertEquals(mockResponse.getTimeStampLocalTimeISO8601(),
                convertToXMLGregorianCalendar(responseMessage.getTimeStampLocalTimeISO8601()));

        // Verify the transformed move information
        assertNotNull(responseMessage.getMoves(), "Move groups should not be null");
        assertEquals(1, responseMessage.getMoves().size()); // Ensure there is one move group
        List<DeckMove> deckMoves = responseMessage.getMoves();
        assertNotNull(deckMoves, "Deck moves should not be null");
        assertEquals(1, deckMoves.size()); // Ensure there is one deck move

        DeckMove transformedMove = deckMoves.get(0);
        assertEquals(WIG_KEY, transformedMove.getWiGkey());
    }
}
