package com.aicon.tos.connect.flows;


import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aicon.TestConstants;
import com.aicon.tos.control.OperatingModeRuleEngine;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.kafka.BufferedKafkaConsumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import util.InMemoryAppender;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CDCSessionTest {

    private CDCController mockController;
    private BufferedKafkaConsumer mockConsumer;
    private CDCSession cdcSession;
    private InMemoryAppender testLogAppender;

    private static final String TEST_TOPIC = "test-topic";

    @BeforeEach
    void setUp() {
        // Access the LoggerContext managed by SLF4J's default binding (Logback)
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Retrieve the Logger for the class you want to test
        Logger logger = loggerContext.getLogger(CDCSession.class);

        // Initialize the InMemoryAppender
        testLogAppender = new InMemoryAppender("TestAppender");
        testLogAppender.start();                  // Start the appender

        // Attach the InMemoryAppender to the logger
        logger.addAppender(testLogAppender);

        // Clear any previously captured logs
        testLogAppender.clear();

        // Ensure the logger level is set to DEBUG (or lower, depending on your needs)
        logger.setLevel(Level.DEBUG);

        // Other test object initialization
        mockController = mock(CDCController.class);
        mockConsumer = mock(BufferedKafkaConsumer.class);
        when(mockConsumer.getTopic()).thenReturn(TEST_TOPIC);

        cdcSession = new CDCSession(mockController, mockConsumer);
        cdcSession.setConsumer(mockConsumer);

        ConfigSettings.setConfigFile(TestConstants.TEST_CONFIG_FULL_FILESPEC);
        ConfigSettings.getInstance().read();

    }


    @AfterEach
    void tearDown() {
        // Get the LoggerContext from SLF4J's Logback implementation
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Retrieve the logger for which the appender was added
        Logger logger = loggerContext.getLogger(OperatingModeRuleEngine.class);

        // Stop and remove the appender
        if (testLogAppender != null) {
            logger.detachAppender(testLogAppender); // Detach appender safely
            testLogAppender.stop();                // Stop the appender
        }
    }

    @Test
    void testCDCSessionInitialization() {
        // Verify the topic and session type are properly initialized
        assertEquals(TEST_TOPIC, cdcSession.getTopic(), "Topic should match the one provided in the constructor.");
        assertEquals("CDC_SESSION", cdcSession.getSessionType(), "Session type should be set to CDC_SESSION.");

        // Verify logs are created during initialization (optionally, if LOG mock is added)
    }

    @Test
    void testRunSetsSessionStateToDoneAndStartsConsumerPolling() {
        // Maak een spy van de CDCSession
        when(mockConsumer.getTopic()).thenReturn(TEST_TOPIC);
        CDCSession cdcSessionSpy = spy(new CDCSession(mockController, mockConsumer));

        // Mock het gedrag van isRunning() om een infinite loop te voorkomen
        doReturn(false).when(cdcSessionSpy).isRunning();

        // Act: Run de sessie
        cdcSessionSpy.run();

        // Assert: Controleer of de methods op de gemockte consumer werden aangeroepen
        verify(mockConsumer, times(1)).initializeConsumer(any());
        verify(mockConsumer, times(1)).startPolling();

        // Controleer dat de consumer is gestopt
        verify(mockConsumer, times(1)).stop();

        // Verifieer dat de sessie is afgerond
        verify(mockController, times(1)).sessionEnded(cdcSessionSpy);
    }

    @Test
    void testStopSessionStopsRunningProcessAndLogsMessage() {
//        int threadCounter = 0;
//        // Act: Run de sessie
//        Thread thread = new Thread(cdcSession,"TestCDCSessionThread");
//        thread.start();

        // Act: Call stopSession
        cdcSession.stopSession();

        // Assert: Verify running flag is set to false
        assertFalse(cdcSession.isRunning(), "Session running flag should be false after calling stopSession().");

        // Assert: Check that the log message is generated
        assertTrue(
                testLogAppender.contains("Stopping CDCSession for topic test-topic."),
                "Log should contain the exact message: 'Stopping CDCSession for topic test-topic.'"
        );
    }

    @Test
    void testFindDateForGKeyInvokesConsumer() {

        // Create the CDCSession and inject the mock consumer
        cdcSession.consumer = mockConsumer; // Inject the mocked consumer

        // Prepare mock return value
        String gKey = "gKey-123";
        Long expectedTimestamp = 123456789L;
        when(mockConsumer.findDateForGKey(gKey)).thenReturn(expectedTimestamp);

        // Act: Call the method
        Long result = cdcSession.findDateForGKey(gKey);

        // Assert: Verify interactions and output
        verify(mockConsumer, times(1)).findDateForGKey(gKey);
        assertEquals(expectedTimestamp, result, "The returned date should match the expected value.");
    }

    @Test
    void testStopSessionLogsMessage() {
        // Act: Call stopSession
        cdcSession.stopSession();

        // Assert: Check that the proper log message was generated
        assertTrue(
                testLogAppender.contains("Stopping CDCSession for topic"),
                "Log should contain the expected message indicating session is stopping."
        );
    }
}