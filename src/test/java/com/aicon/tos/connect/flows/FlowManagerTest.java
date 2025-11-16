package com.aicon.tos.connect.flows;

import com.aicon.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;

import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link FlowManager} class.
 * <p>
 * This test class validates the functionality of the FlowManager, including its singleton behavior,
 * initialization of flow controllers, starting and stopping of controllers, and proper string
 * representation under different scenarios. It uses JUnit annotations for setup and teardown of
 * resources as well as for defining test cases.
 */
class FlowManagerTest {
    private static final Logger LOG = LoggerFactory.getLogger(FlowManagerTest.class);

    private static final String FLOWMANAGERTEST_CONFIG_XML = "conf/mediator/flowmanagertest-config.xml";
    private FlowManager flowManager;

    @BeforeEach
    void setUp() {
        LOG.info("Starting store test including construction of a configuration from scratch...");

        flowManager = FlowManager.forTestConfig(
                TestConstants.PATH_TO_TEST_CONFIG_FILES + FLOWMANAGERTEST_CONFIG_XML);
    }

    @AfterEach
    void tearDown() {

        LOG.info("Finished store test.");
    }

    /**
     * Verifies the singleton behavior of the FlowManager class.
     * <p>
     * This test ensures that multiple calls to FlowManager.getInstance() return the
     * same instance, validating the singleton pattern implementation. The comparison
     * is performed using assertSame to confirm both references point to the same
     * object in memory.
     */
    @Test
    void testSingletonInstance() {
        FlowManager instance1 = FlowManager.getInstance();
        FlowManager instance2 = FlowManager.getInstance();
        assertSame(instance1, instance2, "FlowManager should be a singleton");
    }

    /**
     * Tests the initialization process of the FlowManager to ensure it creates and configures
     * the correct number of flow controllers.
     * <p>
     * This test method invokes the {@code init} method of the {@code flowManager} instance
     * and verifies if the {@code controllers} collection is populated as expected.
     * <p>
     * Assertions:
     * - Validates that the {@code controllers} collection is not empty after initialization.
     * - Confirms that the {@code controllers} collection contains exactly two flow controllers.
     */
    @Test
    void testInitCreatesFlowControllers() {
        flowManager.init();

        assertFalse(flowManager.controllers.isEmpty(), "Flow controllers should be initialized");
        assertEquals(3, flowManager.controllers.size(), "There should be exactly three flow controllers");
    }

    /**
     * Tests the start and stop functionality of the controllers managed by the FlowManager.
     * <p>
     * This method verifies the following:
     * - After invoking the `start` method on the FlowManager:
     * - All threads associated with the Canary Controllers are started and alive.
     * - After invoking the `stop` method on the FlowManager:
     * - All threads associated with the controllers, including Canary Controllers, are stopped.
     * <p>
     * Assertions and validations performed during the test include:
     * - Ensures that each thread for the Canary Controllers is alive after starting FlowManager.
     * - Ensures that each thread for the controllers is no longer alive after stopping FlowManager.
     * <p>
     * Additionally, there is a short delay introduced using `Thread.sleep` to allow all
     * controllers to fully start before assertions are checked.
     * <p>
     * Logs are produced to indicate the start and stop process for better traceability
     * during test execution.
     */

    @Test
    void testStartAndStopControllers() {
        flowManager.init();

        LOG.info(">>>>> Starting ..... ");
        flowManager.start();
        flowManager.canaryControllers.forEach((controller, thread) -> {
            assertTrue(thread.isAlive(), "Canary Controller thread should be started");
        });

        // Wait for all to be started...
        waitSeconds(2, "for all controllers to start...");

        LOG.info(">>>>> Stopping ..... ");
        flowManager.stop();

        flowManager.controllers.forEach((controller, thread) -> {
            assertFalse(controller.isRunning(), "Controller threads should be stopped after FlowManager stop");
        });
        flowManager.canaryControllers.forEach((controller, thread) -> {
            assertFalse(controller.isRunning(), "CanaryController threads should be stopped after FlowManager stop");
        });
        flowManager.cdcControllers.forEach((controller, thread) -> {
            assertFalse(controller.isRunning(), "CDCController threads should be stopped after FlowManager stop");
        });
    }

    @Test
    void testToStringNotInitialized() throws Exception {
        resetSingleton(FlowManager.class, "instance");
        flowManager = FlowManager.getInstance();

        assertEquals("FlowManager not initialized yet or not properly configured", flowManager.toString());
    }

    @Test
    void testToStringWithInitializedControllers() {
        flowManager.init();

        String result = flowManager.toString();

        assertTrue(result.contains("FlowControllers:"), "String output should contain 'FlowControllers'");
        assertTrue(result.contains("ControlFlow"), "String output should contain the flow name 'TOS-CONTROL'");
    }

    // Reset the singleton instance via reflection
    private void resetSingleton(Class<?> clazz, String fieldName) throws Exception {
        Field instanceField = clazz.getDeclaredField(fieldName);
        instanceField.setAccessible(true);
        instanceField.set(null, null);
    }
}
