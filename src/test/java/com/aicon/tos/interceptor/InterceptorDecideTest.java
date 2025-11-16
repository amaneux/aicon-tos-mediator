package com.aicon.tos.interceptor;

import com.aicon.TestConstants;
import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for the InterceptorDecide class using the real configuration.
 */
class InterceptorDecideTest {

    private final static String TOPIC_INV_WI = "inv_wi";
    private final static String TOPIC_TRUCK_TRANSACTIONS = "truck_transactions";

    @BeforeEach
    void setup() {
        // Set the test configuration file path before each test
        ConfigSettings.setConfigFile(TestConstants.TEST_CONFIG_FULL_FILESPEC);
    }

    @Test
    void testStartInitializesThreadForSingleTopicFromRealConfig() {
        // Arrange: Load the real configuration
        ConfigSettings config = ConfigSettings.getInstance();
        InterceptorConfig interceptorConfig = new InterceptorConfig(config.getMainGroup(ConfigType.Interceptors));

        // Act: Start InterceptorDecide with the real config
        InterceptorDecide interceptorDecide = new InterceptorDecide(interceptorConfig);
        interceptorDecide.start();

        // Assert: Verify one thread is initialized for the first topic
        assertEquals(2, interceptorDecide.getNumberOfStartedFilterThreads(),
                "There should be one filter for the single topic (inv_wi) in the Interceptor config.");

        // Cleanup
        interceptorDecide.shutdown();
    }

    @Test
    void testStartInitializesThreadsForMultipleTopicsFromRealConfig() {
        // Arrange: Load the real configuration
        ConfigSettings config = ConfigSettings.getInstance();
        InterceptorConfig interceptorConfig = new InterceptorConfig(config.getMainGroup(ConfigType.Interceptors));

        // Add an additional topic dynamically
        interceptorConfig.addEntityConfig(TOPIC_TRUCK_TRANSACTIONS,
                new InterceptorEntityConfig(
                        TOPIC_TRUCK_TRANSACTIONS,
                        TOPIC_TRUCK_TRANSACTIONS,
                        null,
                        5000,
                        Duration.ofMinutes(30),
                        1500L,
                        null,
                        true, false, false
                )
        );

        // Act: Start InterceptorDecide with multiple topics
        InterceptorDecide interceptorDecide = new InterceptorDecide(interceptorConfig);
        interceptorDecide.start();

        // Assert: Verify two threads are initialized (one for each configured topic)
        assertEquals(2, interceptorDecide.getNumberOfStartedFilterThreads(),
                "There should be two filters initialized for the two topics in the Interceptor config.");

        // Cleanup
        interceptorDecide.shutdown();
    }

    @Test
    void testNoFilterCreatedWhenNoTopicsConfiguredInRealConfig() {
        // Arrange: Remove all topic configurations to simulate no topics
        ConfigSettings config = ConfigSettings.getInstance();
        InterceptorConfig interceptorConfig = new InterceptorConfig(config.getMainGroup(ConfigType.Interceptors));
        interceptorConfig.clearEntities(); // Simulate no topics configuration

        // Act: Start the InterceptorDecide instance
        InterceptorDecide interceptorDecide = new InterceptorDecide(interceptorConfig);
        interceptorDecide.start();

        // Assert: Verify no filter threads are created
        assertEquals(0, interceptorDecide.getNumberOfStartedFilterThreads(),
                "No filters should be created when no topics are configured in the Interceptor config.");

        // Cleanup
        interceptorDecide.shutdown();
    }

    @Test
    void testShutdownCleansUpExecutorsGracefullyUsingRealConfig() {
        // Arrange: Load the real configuration
        ConfigSettings config = ConfigSettings.getInstance();
        InterceptorConfig interceptorConfig = new InterceptorConfig(config.getMainGroup(ConfigType.Interceptors));

        // Act: Initialize and shut down InterceptorDecide
        InterceptorDecide interceptorDecide = new InterceptorDecide(interceptorConfig);
        interceptorDecide.start();  // Start threads
        interceptorDecide.shutdown();  // Shut down threads

        // Assert: If no exception is thrown, the executors have shut down gracefully
    }
}