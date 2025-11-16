package util;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aicon.tos.shared.util.TimeSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

class TimeSyncServiceTest {

    private static final String TEST_APPENDER_NAME = "TestAppender";

    @BeforeEach
    public void setUp() {
        // Access the LoggerContext managed by SLF4J's default binding (Logback)
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Initialize and configure the InMemoryAppender
        InMemoryAppender testLogAppender = new InMemoryAppender(TEST_APPENDER_NAME);
        testLogAppender.setContext(loggerContext);
        testLogAppender.start();

        // Retrieve the Logger for the class you want to test
        Logger logger = loggerContext.getLogger(TimeSyncServiceTest.class);

        // OPTIONAL: Attach testLogAppender only to application-specific logger (if necessary)
        Logger appLogger = loggerContext.getLogger("com.aicon.tos.control");
        appLogger.addAppender(testLogAppender);
        appLogger.setLevel(Level.DEBUG);

//        // Ensure the appender is not already added
//        logger.detachAppender(testLogAppender);  // Remove if present
//        logger.addAppender(testLogAppender);     // Attach the new appender

        // Clear previously captured logs (current logs list in appender)
        testLogAppender.clear();
    }


    static Stream<Arguments> provideTestCases() {
        return Stream.of(
                // @formatter:off
                //                  reqSendAicon, reqReceivedTos, resSendTos, resReceivedAicon, expectedSyncTime
                Arguments.of("10:00:00", "10:00:05", "10:00:10", "10:00:15", 0L),
                Arguments.of("09:00:00", "09:00:05", "09:00:10", "09:00:15", 0L),
                Arguments.of("09:00:00", "09:00:04", "09:00:08", "09:00:12", 0L),
                Arguments.of("09:00:00", "09:00:03", "09:00:07", "09:00:11", -500L),
                Arguments.of("09:00:10", "09:00:05", "09:00:07", "09:00:12", -5000L),
                Arguments.of("09:00:05", "09:00:10", "09:00:07", "09:00:12", 0L),
                Arguments.of("13:14:00", "13:14:18", "13:14:20", "13:14:06", 16000L)
                // @formatter:on
        );
    }

    @ParameterizedTest
    @MethodSource("provideTestCases")
    void testDetermineSyncTime(final String argReqSendAicon, final String argReqReceivedTos, final String argResSendTos, final String argResReceivedAicon, final long expectedSyncTime) {
        final XMLGregorianCalendar reqSendAicon = TimeSyncServiceTestHelper.stringToXMLGregorianCalendar(argReqSendAicon);
        final XMLGregorianCalendar reqReceivedTos = TimeSyncServiceTestHelper.stringToXMLGregorianCalendar(argReqReceivedTos);
        final XMLGregorianCalendar resSendTos = TimeSyncServiceTestHelper.stringToXMLGregorianCalendar(argResSendTos);
        final XMLGregorianCalendar resReceivedAicon = TimeSyncServiceTestHelper.stringToXMLGregorianCalendar(argResReceivedAicon);

        Long reqSendAiconMillis = TimeSyncServiceTestHelper.xMLGregorianCalendarToMillis(reqSendAicon);
        Long reqReceivedTosMillis = TimeSyncServiceTestHelper.xMLGregorianCalendarToMillis(reqReceivedTos);
        Long resSendTosMillis = TimeSyncServiceTestHelper.xMLGregorianCalendarToMillis(resSendTos);
        Long resReceivedAiconMillis = TimeSyncServiceTestHelper.xMLGregorianCalendarToMillis(resReceivedAicon);

        // Act
        final long syncTime = TimeSyncService.determineSyncTime(reqSendAiconMillis, reqReceivedTosMillis, resSendTosMillis, resReceivedAiconMillis);

        // Assert
        final String format = String.format("Sync time %d should match the expected value %d", syncTime, expectedSyncTime);
        Assertions.assertEquals(expectedSyncTime, syncTime, format);
    }

    @Test
    void testDynamicAppenderIntegration() {
        // Arrange: Access the LoggerContext for Logback
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Retrieve the logger for TimeSyncService
        Logger timeSyncServiceLogger = context.getLogger(TimeSyncService.class);

        // Detach all appenders from the logger to ensure test isolation
        timeSyncServiceLogger.detachAndStopAllAppenders();
        timeSyncServiceLogger.setLevel(Level.DEBUG); // Ensure DEBUG level is set
        timeSyncServiceLogger.setAdditive(false); // Prevent log message propagation to parent loggers

        // Create and configure the InMemoryAppender
        InMemoryAppender dynamicAppender = new InMemoryAppender("DynamicInMemoryAppender");
        dynamicAppender.setContext(context);
        dynamicAppender.start(); // Start the appender

        // Attach the dynamic appender to the TimeSyncService logger
        timeSyncServiceLogger.addAppender(dynamicAppender);

        // Act: Log a message using the TimeSyncService logger
        String testMessage = "Dynamic appender test message";
        timeSyncServiceLogger.debug(testMessage); // Log the test message

        // Assert: Verify that the log message was captured by the appender
        List<String> logMessages = dynamicAppender.getLogMessages();
        assertTrue(logMessages.stream().anyMatch(msg -> msg.contains(testMessage)),
                "The dynamic appender did not capture the expected log message");

        // Cleanup: Detach and stop the custom appender to avoid test side effects
        timeSyncServiceLogger.detachAppender(dynamicAppender);
        dynamicAppender.stop();
    }
}