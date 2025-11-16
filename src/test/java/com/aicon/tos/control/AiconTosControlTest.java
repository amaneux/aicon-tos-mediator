package com.aicon.tos.control;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aicon.TestConstants;
import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusConsumer;
import com.aicon.tos.shared.kafka.AiconTosControlProducer;
import com.aicon.tos.shared.kafka.AiconUserControlConsumer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.schema.AiconTosConnectionStatusMessage;
import com.aicon.tos.shared.schema.AiconUserControlMessage;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import util.InMemoryAppender;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the AiconTosControl class.
 */
class AiconTosControlTest {
    private static final String AICONTOSCONTROL_CONFIG_XML =
            TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/aicontoscontroltest-config.xml";
    private static final String TEST_APPENDER_NAME = "TestAppender";

    private AiconTosControl aiconTosControl;
    private AiconTosControlProducer tosControlProducer;
    private AiconTosConnectionStatusConsumer connectionStatusConsumer;
    private AiconUserControlConsumer userControlConsumer;
    private InMemoryAppender testLogAppender;
    private ConfigSettings configSettings;
    private DataStore dataStore;

    /**
     * Set up mock objects and initialize the AiconTosControl instance before each test.
     */
    @BeforeEach
    void setUp() {
        // Get LoggerContext
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Create and configure the appender
        testLogAppender = new InMemoryAppender(TEST_APPENDER_NAME);
        testLogAppender.setContext(loggerContext);
        testLogAppender.start();

        // Attach to ROOT logger to capture ALL logs, including application and system logs
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(testLogAppender);
        rootLogger.setLevel(Level.DEBUG);

        // OPTIONAL: Attach testLogAppender only to application-specific logger (if necessary)
        Logger appLogger = loggerContext.getLogger("com.aicon.tos.control");
        appLogger.addAppender(testLogAppender);
        appLogger.setLevel(Level.DEBUG);

        // Clear logs
        testLogAppender.clear();

        // Mock dependencies and initialize the test subject
        ConfigSettings.setConfigFile(AICONTOSCONTROL_CONFIG_XML);
        configSettings = ConfigSettings.getInstance();
        configSettings.read();

        tosControlProducer = mock(AiconTosControlProducer.class);
        connectionStatusConsumer = mock(AiconTosConnectionStatusConsumer.class);
        userControlConsumer = mock(AiconUserControlConsumer.class);
        dataStore = mock(DataStore.class);
        aiconTosControl = new AiconTosControl(dataStore, tosControlProducer, connectionStatusConsumer, userControlConsumer);
    }

    @AfterEach
    void tearDown() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        Logger rootLogger = loggerContext.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);

        if (testLogAppender != null) {
            rootLogger.detachAppender(testLogAppender);
            testLogAppender.stop();
        }
    }

    /**
     * Test the `sendTosControlMessage` method using a mocked producer.
     */
    @Test
    void testSendTosControlMessage() {
        // Arrange
        String id = "123";
        OperatingMode operatingMode = OperatingMode.ON;
        Boolean ignore = false;
        when(dataStore.getTimeSync()).thenReturn(345L); // Stub the return value

        // Act
        aiconTosControl.sendAiconTosControlMessage(id, operatingMode, ignore, "Test");

        // Assert
        verify(tosControlProducer, times(1))
                .sendAiconTosControlMessage(
                        eq(id), // ignore SonarCube here
                        eq(operatingMode.name()),
                        eq(Boolean.FALSE),
                        eq(345L), // Stubbed timeSync value
                        eq("Test")
                );
    }

    /**
     * Test processing connection status and CDC messages when null timestamps are received,
     * ensuring that an appropriate log message is generated.
     */
    @Test
    void testProcessConnectionStatus_shouldLogThis() {
        String expectedCDCMessage;

        AiconTosConnectionStatusMessage mockMessage = mock(AiconTosConnectionStatusMessage.class);

        when(mockMessage.getConnectionStatus()).thenReturn(ConnectionStatus.NOK);
        when(mockMessage.getCdcOk()).thenReturn(!aiconTosControl.getCurrentCdcOk());

        if (aiconTosControl.getCurrentCdcOk()) {
            expectedCDCMessage = "CDC true=>false";
        } else {
            expectedCDCMessage = "CDC false=>true";
        }

        ConsumerRecord<String, AiconTosConnectionStatusMessage> consumerRecord =
                new ConsumerRecord<>(KafkaConfig.AICON_TOS_CONNECTION_STATUS_TOPIC, 0, 0L, null, mockMessage);
        ConsumerRecords<String, AiconTosConnectionStatusMessage> consumerRecords =
                new ConsumerRecords<>(Collections.singletonMap(null, Collections.singletonList(consumerRecord)));

        // Mock the connectionStatusConsumer to return the mocked consumerRecords
        when(connectionStatusConsumer.pollMessages()).thenReturn(consumerRecords);

        aiconTosControl.processConnectionStatusAndCDCMessages();

        // Verify log messages
        boolean isLogged = testLogAppender.getLogMessages().stream()
                .anyMatch(message -> message.contains(expectedCDCMessage));

        assertTrue(isLogged, "Log should contain message " + expectedCDCMessage);
        // Verify log messages
        isLogged = testLogAppender.getLogMessages().stream()
                .anyMatch(message -> message.contains("New operating mode:"));
        assertTrue(isLogged, "Log should contain message New operating mode:");

        // Verify that the correct methods were called
        verify(connectionStatusConsumer, times(1)).pollMessages();
    }

    /**
     * Test when CDC data is missing, ensuring that no further interactions occur.
     */
    @Test
    void testProcessConnectionStatusAndCDCMessages_EmptyCDCData() {
        ConsumerRecords<String, AiconTosConnectionStatusMessage> emptyRecords = new ConsumerRecords<>(
                Collections.emptyMap());
        when(connectionStatusConsumer.pollMessages()).thenReturn(emptyRecords);

        aiconTosControl.processConnectionStatusAndCDCMessages();
        verify(connectionStatusConsumer, times(1)).pollMessages();
        verifyNoMoreInteractions(connectionStatusConsumer);
    }

    /**
     * Test if current ConnectionStatus and CDC status values are updated when messages are processed.
     */
    @Test
    void testProcessConnectionStatusAndCDCMessages_ChangingCurrentValues() {
        // Mock the AiconTosConnectionStatusMessage message
        AiconTosConnectionStatusMessage mockMessage = mock(AiconTosConnectionStatusMessage.class);

        // Set values returned by the mock
        when(mockMessage.getConnectionStatus()).thenReturn(ConnectionStatus.OK);
        when(mockMessage.getCdcOk()).thenReturn(Boolean.TRUE);

        // Create ConsumerRecord and ConsumerRecords objects to simulate messages
        ConsumerRecord<String, AiconTosConnectionStatusMessage> consumerRecord =
                new ConsumerRecord<>(KafkaConfig.AICON_TOS_CONNECTION_STATUS_TOPIC, 0, 0L, null, mockMessage);
        ConsumerRecords<String, AiconTosConnectionStatusMessage> consumerRecords =
                new ConsumerRecords<>(Collections.singletonMap(null, Collections.singletonList(consumerRecord)));

        // Mock the connectionStatusConsumer to return the mocked consumerRecords
        when(connectionStatusConsumer.pollMessages()).thenReturn(consumerRecords);

        // Store the current values before calling the method
        ConnectionStatus previousConnectionStatus = aiconTosControl.getCurrentConnectionStatus();
        Boolean previousCdcOk = aiconTosControl.getCurrentCdcOk();

        // Call the method that should update the values
        aiconTosControl.processConnectionStatusAndCDCMessages();

        // Check if the values have been updated
        assertNotEquals(previousConnectionStatus, aiconTosControl.getCurrentConnectionStatus(),
                "The currentConnectionStatus should have been updated.");
        assertNotEquals(previousCdcOk, aiconTosControl.getCurrentCdcOk(),
                "The currentCdcOk value should have been updated.");

        // Verify that the mocked methods were called
        verify(connectionStatusConsumer, times(1)).pollMessages();
        verifyNoMoreInteractions(connectionStatusConsumer);
    }

    /**
     * Test if the current UserOperatingMode is updated when user control messages are processed.
     */
    @Test
    void testProcessUserOperatingModeMessages_ChangingCurrentValues() {
        // Mock the AiconUserControlMessage message
        AiconUserControlMessage mockMessage = mock(AiconUserControlMessage.class);

        // Set values returned by the mock
        when(mockMessage.getUserOperatingMode()).thenReturn(UserOperatingMode.ON);

        // Create ConsumerRecord and ConsumerRecords objects to simulate messages
        ConsumerRecord<String, AiconUserControlMessage> consumerRecord =
                new ConsumerRecord<>("AiconUserControlMessage", 0, 0L, null, mockMessage);
        ConsumerRecords<String, AiconUserControlMessage> consumerRecords =
                new ConsumerRecords<>(Collections.singletonMap(null, Collections.singletonList(consumerRecord)));

        // Mock the userControlConsumer to return the mocked consumerRecords
        when(userControlConsumer.pollMessages()).thenReturn(consumerRecords);

        // Store the current values before calling the method
        UserOperatingMode previousUserOperatingMode = aiconTosControl.getCurrentUserOperatingMode();

        // Call the method that should update the values
        aiconTosControl.processUserControlMessages();

        // Check if the values have been updated
        assertNotEquals(previousUserOperatingMode, aiconTosControl.getCurrentUserOperatingMode(),
                "The currentUserOperatingMode should have been updated.");

        // Verify that the mocked methods were called
        verify(userControlConsumer, times(1)).pollMessages();
        verifyNoMoreInteractions(userControlConsumer);
    }
}
