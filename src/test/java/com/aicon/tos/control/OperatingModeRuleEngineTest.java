package com.aicon.tos.control;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aicon.TestConstants;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import util.InMemoryAppender;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the OperatingModeRuleEngine class.
 */
class OperatingModeRuleEngineTest {

    private InMemoryAppender appender;

    /**
     * Provide various test cases for OperatingMode determination using a parameterized test.
     *
     * @return Stream of arguments for different combinations of connection status, CDC status, and user operating modes.
     */
    static Stream<Arguments> provideOperatingModeDeterminationTestCases() {
        return Stream.of(
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.ON, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.ON, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.ON, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.OFF, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.OFF, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.OFF, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.AUTO, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.AUTO, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.AUTO, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.AUTO, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, false, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.ON, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.ON, UserOperatingMode.AUTO, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.ON, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.OFF, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.OFF, UserOperatingMode.AUTO, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.OFF, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.AUTO, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.AUTO, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.AUTO, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.OK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.AUTO, OperatingMode.ON),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, false, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.OK, ConnectionStatus.NOK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.ON),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.ON),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.ON),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.ON, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.ON, UserOperatingMode.AUTO, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.ON, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.OFF, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.OFF, UserOperatingMode.AUTO, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.OFF, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.AUTO, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.AUTO, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.AUTO, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, false, UserOperatingMode.SHADOW, UserOperatingMode.AUTO, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, true, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, true, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, true, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, false, true, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, false, UserOperatingMode.ON, UserOperatingMode.ON, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, false, UserOperatingMode.OFF, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, false, UserOperatingMode.AUTO, UserOperatingMode.AUTO, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, false, UserOperatingMode.SHADOW, UserOperatingMode.SHADOW, OperatingMode.OFF),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.ON, UserOperatingMode.OFF, OperatingMode.OFF),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.ON, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.ON, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.OFF, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.OFF, UserOperatingMode.AUTO, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.OFF, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.AUTO, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.AUTO, UserOperatingMode.OFF, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.AUTO, UserOperatingMode.SHADOW, OperatingMode.SHADOW),

                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.ON, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.OFF, OperatingMode.SHADOW),
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.NOK, true, true, UserOperatingMode.SHADOW, UserOperatingMode.AUTO, OperatingMode.SHADOW)
        );
        // @formatter:on
    }

    /**
     * Provide test cases that should not happen in practice.
     *
     * @return Stream of arguments for edge case combinations.
     */
    static Stream<Arguments> provideOperatingModeDeterminationNonExistingTestCases() {
        return Stream.of(
                // Edge cases that should not happen in practice
                Arguments.of(ConnectionStatus.NOK, ConnectionStatus.OK, false, true, UserOperatingMode.OFF, UserOperatingMode.ON, OperatingMode.OFF)
        );
    }

    // Method to generate combinations with the last two parameters equal
    private static Stream<Arguments> provideCombinationsWithEqualUserOperatingMode() {
        List<Arguments> argumentsList = new ArrayList<>();

        for (ConnectionStatus currentConnectionStatus : ConnectionStatus.values()) {
            for (ConnectionStatus connectionStatus : ConnectionStatus.values()) {
                for (boolean currentCdcOk : new boolean[]{true, false}) {
                    for (boolean cdcOk : new boolean[]{true, false}) {
                        // Filter to keep only cases where cdcOk is different from currentCdcOk
                        if (!cdcOk == currentCdcOk) {
                            for (UserOperatingMode userOperatingMode : UserOperatingMode.values()) {
                                argumentsList.add(Arguments.of(
                                        currentConnectionStatus,
                                        connectionStatus,
                                        currentCdcOk,
                                        cdcOk,
                                        userOperatingMode, // currentUserOperatingMode
                                        userOperatingMode // userOperatingMode (same as currentUserOperatingMode)
                                ));
                            }
                        }
                    }
                }
            }
        }

        // Convert the list to a stream and return it
        return argumentsList.stream();
    }

    /**
     * Generate combinations where the first four parameters are constant and the last two vary.
     *
     * @return Stream of arguments with constant first four parameters.
     */
    private static Stream<Arguments> provideCombinationsWithConstantFirstFour() {
        List<Arguments> argumentsList = new ArrayList<>();

        for (ConnectionStatus currentConnectionStatus : ConnectionStatus.values()) {
            for (ConnectionStatus connectionStatus : ConnectionStatus.values()) {
                for (boolean currentCdcOk : new boolean[]{true, false}) {
                    for (boolean cdcOk : new boolean[]{true, false}) {
                        for (UserOperatingMode currentUserOperatingMode : UserOperatingMode.values()) {
                            for (UserOperatingMode userOperatingMode : UserOperatingMode.values()) {
                                // Filter to keep only cases where the operating modes are different
                                if (!userOperatingMode.equals(currentUserOperatingMode)) {
                                    argumentsList.add(Arguments.of(
                                            currentConnectionStatus,
                                            connectionStatus,
                                            currentCdcOk,
                                            cdcOk,
                                            currentUserOperatingMode,
                                            userOperatingMode
                                    ));
                                }
                            }
                        }
                    }
                }
            }
        }

        // Convert the list to a stream and return it
        return argumentsList.stream();
    }

    /**
     * Combine the two streams to provide all possible combinations.
     *
     * @return Stream of all possible combinations.
     */
    private static Stream<Arguments> provideAllCombinations() {
        return Stream.concat(
                provideCombinationsWithEqualUserOperatingMode(),
                provideCombinationsWithConstantFirstFour()
        );
    }

    @Test
    void newOperatingMode() {
    }

    /**
     * Set up mock objects before each test.
     */
    @BeforeEach
    void setUpEach() {
        // 1. Set up the configuration file
        ConfigSettings.setConfigFile(TestConstants.TEST_CONFIG_FULL_FILESPEC);

        // 2. Access the LoggerContext managed by SLF4J's default binding (Logback)
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // 3. Retrieve the Logger for the class you want to test
        Logger logger = loggerContext.getLogger(OperatingModeRuleEngine.class);

        // 4. Initialize the InMemoryAppender
        appender = new InMemoryAppender("TestAppender");
        appender.setContext(loggerContext);
        appender.start();                  // Start the appender

        // 5. Attach the InMemoryAppender to the logger
        logger.addAppender(appender);

        // 6. Clear any previously captured logs
        appender.clear();
    }


    @AfterEach
    void tearDown() {
        // Get the LoggerContext from SLF4J's Logback implementation
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Retrieve the logger for which the appender was added
        Logger logger = loggerContext.getLogger(OperatingModeRuleEngine.class);

        // Stop and remove the appender
        if (appender != null) {
            logger.detachAppender(appender); // Detach appender safely
            appender.stop();                // Stop the appender
        }
    }




    /**
     * Test the determination of the operating mode based on different combinations of inputs.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param connectionStatus         The new connection status.
     * @param currentCdcOk             The current CDC OK status.
     * @param cdcOk                    The new CDC OK status.
     * @param currentUserOperatingMode The current user operating mode.
     * @param userOperatingMode        The new user operating mode.
     * @param expectedOperatingMode    The expected operating mode result.
     */
    @ParameterizedTest
    @MethodSource("provideOperatingModeDeterminationTestCases")
    void testDetermineOperatingMode(
            ConnectionStatus currentConnectionStatus, ConnectionStatus connectionStatus,
            Boolean currentCdcOk, Boolean cdcOk,
            UserOperatingMode currentUserOperatingMode, UserOperatingMode userOperatingMode,
            OperatingMode expectedOperatingMode
    ) {

        OperatingModeRuleEngine engine = OperatingModeRuleEngine.getInstance(true);

        OperatingMode result = engine.newOperatingMode(currentConnectionStatus, connectionStatus,
                currentCdcOk, cdcOk,
                currentUserOperatingMode, userOperatingMode);
        assertEquals(expectedOperatingMode, result,
                String.format("Expected: %s, Actual: %s", expectedOperatingMode.name(), result.name()));
    }

    /**
     * Test the determination of the operating mode for cases that should not happen.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param connectionStatus         The new connection status.
     * @param currentCdcOk             The current CDC OK status.
     * @param cdcOk                    The new CDC OK status.
     * @param currentUserOperatingMode The current user operating mode.
     * @param userOperatingMode        The new user operating mode.
     * @param expectedOperatingMode    The expected operating mode result.
     */
    @ParameterizedTest
    @MethodSource("provideOperatingModeDeterminationNonExistingTestCases")
    void testDetermineOperatingModeNonExisting(
            ConnectionStatus currentConnectionStatus, ConnectionStatus connectionStatus,
            Boolean currentCdcOk, Boolean cdcOk,
            UserOperatingMode currentUserOperatingMode, UserOperatingMode userOperatingMode,
            OperatingMode expectedOperatingMode
    ) {

        OperatingModeRuleEngine engine = OperatingModeRuleEngine.getInstance(true);

        OperatingMode result = engine.newOperatingMode(currentConnectionStatus, connectionStatus,
                currentCdcOk, cdcOk,
                currentUserOperatingMode, userOperatingMode);
        assertEquals(expectedOperatingMode, result,
                String.format("Expected: %s, Actual: %s", expectedOperatingMode.name(), result.name()));

        // Verify log messages
        boolean isErrorLogged = appender.getLogMessages().stream()
                .anyMatch(message -> message.contains("No matching rule found for the given inputs:"));

        assertTrue(isErrorLogged, "Expected log message not found in logs.");

    }

    @ParameterizedTest
    @MethodSource("provideAllCombinations")
    void testDetermineOperatingModeForAllCombinations(
            ConnectionStatus currentConnectionStatus, ConnectionStatus connectionStatus,
            Boolean currentCdcOk, Boolean cdcOk,
            UserOperatingMode currentUserOperatingMode, UserOperatingMode userOperatingMode
    ) {
        OperatingModeRuleEngine engine = OperatingModeRuleEngine.getInstance(true);
        engine.newOperatingMode(currentConnectionStatus, connectionStatus,
                currentCdcOk, cdcOk,
                currentUserOperatingMode, userOperatingMode);
        // Verify log messages
        boolean isNoErrorLogged = appender.getLogMessages().stream()
                .noneMatch(message -> message.contains(
                        "ERROR com.aicon.aicon_tos_control.OperatingModeRuleEngine"));

        assertTrue(isNoErrorLogged, "Error log should contain message about \"No matching rule found for the given inputs:...\".");
    }
}