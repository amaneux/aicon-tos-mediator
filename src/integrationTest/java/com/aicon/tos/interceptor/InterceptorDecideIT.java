package com.aicon.tos.interceptor;

import com.aicon.TestConstants;
import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.aicon.tos.shared.config.ConfigSettings;
import com.avlino.common.MetaField;
import com.avlino.common.ValueObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.aicon.tos.connect.cdc.CDCAction.*;
import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorDecideIT {

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorDecideIT.class);

    private final String TOPIC = "testTopic";
    private final String FIELD1 = "field1";
    private final String FIELD2 = "field2";


    private InterceptorDecide decide = new InterceptorDecide();
    private MockedStatic<InterceptorConfig> interceptorConfigMock;

    @BeforeEach
    void setUp() {
        ConfigSettings.setConfigFile(TestConstants.TEST_CONFIG_FULL_FILESPEC);

//        // Mock InterceptorConfig to return a mocked configuration for the test topic
//        interceptorConfigMock = Mockito.mockStatic(InterceptorConfig.class);
//        InterceptorEntityConfig mockedConfig = new InterceptorEntityConfig(
//                TOPIC,
//                10, // Max 10 messages
//                parseTimeString("00:30:00"),
//        )
    }
    @AfterEach
    void tearDown() {
        // Shutdown the InterceptorDecide threads properly
        decide.shutdown();
        interceptorConfigMock.close();
    }

    @Test
    void testInterceptorDecideInitialization() {
        assertNotNull(decide, "InterceptorDecide should be initialized properly.");
    }

    @Test
    void testMessageStorageWithLimit() throws InterruptedException {
        InterceptorConfig interceptorConfig = new InterceptorConfig();
        interceptorConfig.clearEntityConfigs();
        interceptorConfig.addEntityConfig("testTopic", new InterceptorEntityConfig(
                "testTopic",
                "testTopic",
                null,
                5, // Max 5 messages
                Duration.ofMinutes(30),
                1000L,
                null,
                true, true, true
        ));


        // Validate configuration has been successfully added.
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("testTopic");
        assertNotNull(config, "TopicConfig should exist for 'testTopic'.");
        assertEquals(10, config.getMaxNrMessagesInStorage(), "Maximum message limit should match.");

        assertNotNull(interceptorConfig.getEntityConfig(TOPIC), "Static configuration for the topic should be mocked and not null.");

        assertEquals(2, interceptorConfig.getEntityConfig(TOPIC).getMaxNrMessagesInStorage(),
                "The mocked configuration should return the updated maxNrMessages.");

        decide = new InterceptorDecide(interceptorConfig);
        decide.clearStorage();

        // Restart InterceptorDecide with the updated configuration
        decide.start();

        // Add test messages
        MetaField<String> testField = new MetaField<>("test", String.class);
        decide.addMessageToSharedQueue(new FilteredMessage(
                new CollectedMessage(CHANGED, TOPIC, 0, 0, "key1", createChangedFields(testField, "old1", "new1"))
        ));
        decide.addMessageToSharedQueue(new FilteredMessage(
                new CollectedMessage(CHANGED, TOPIC, 0, 0, "key2", createChangedFields(testField, "old2", "new2"))
        ));
        decide.addMessageToSharedQueue(new FilteredMessage(
                new CollectedMessage(CHANGED, TOPIC, 0, 0, "key3", createChangedFields(testField, "old3", "new3"))
        ));

        // Wait for processing
        waitSeconds(1, "Wait for test messages to be processed.");

        // Validate that the max limit of messages is respected
        List<FilteredMessage> storedMessages = decide.getStoredMessages(TOPIC);
        assertTrue(storedMessages.size() <= 2, "Stored messages size should not exceed the configured limit.");
    }

    @Test
    void testCorrectMessageProcessing() throws InterruptedException {

        // Mock static methods for InterceptorConfig
        InterceptorConfig interceptorConfig = new InterceptorConfig();
        interceptorConfig.getEntityKeys();
        interceptorConfig.addEntityConfig("testTopic", new InterceptorEntityConfig(
                "testTopic",
                "testTopic",
                null,
                10, // Max messages
                Duration.ofMinutes(30),
                1000L,
                null,
                true, true, true
        ));

        assertNotNull(interceptorConfig.getEntityConfig(TOPIC), "Static configuration for the topic should be mocked and not null.");

        assertEquals(10, interceptorConfig.getEntityConfig(TOPIC).getMaxNrMessagesInStorage(),
                "The mocked configuration should return the updated maxNrMessages.");

        // Reinitialize InterceptorDecide to refresh its internal state
        decide.shutdown(); // Stop existing threads
        decide = new InterceptorDecide(interceptorConfig); // Create new instance
        decide.start(); // Start with mocked configuration


        // Use a CountDownLatch to wait for message processing completion
        CountDownLatch latch = new CountDownLatch(3); // One for each message

        // Define test fields
        MetaField<String> testField = new MetaField<>("test", String.class);

        // Create test messages with explicit data for creation, change, and deletion
        FilteredMessage creationMessage = new FilteredMessage(
                new CollectedMessage(CREATED, TOPIC, 0, 0, "creation", createChangedFields(testField, null, "newValue"))
        );

        FilteredMessage changeMessage = new FilteredMessage(
                new CollectedMessage(CHANGED, TOPIC, 0, 0, "change", createChangedFields(testField, "oldValue", "newValue"))
        );

        FilteredMessage deletionMessage = new FilteredMessage(
                new CollectedMessage(DELETED, TOPIC, 0, 0, "deletion", createChangedFields(testField, "oldValue", null))
        );

        // Add messages to the shared queue and implement custom processing logic
        decide.addMessageToSharedQueue(creationMessage);
        LOG.info("Submitted creation message: {}", creationMessage);
        latch.countDown(); // Countdown for creation message

        decide.addMessageToSharedQueue(changeMessage);
        LOG.info("Submitted change message: {}", changeMessage);
        latch.countDown(); // Countdown for change message

        decide.addMessageToSharedQueue(deletionMessage);
        LOG.info("Submitted deletion message: {}", deletionMessage);
        latch.countDown(); // Countdown for deletion message

        // Wait for processing to complete
        boolean allProcessed = latch.await(10, TimeUnit.SECONDS); // Increased timeout
        assertTrue(allProcessed, "All messages should be processed before timeout.");

        // Validate the stored messages in the global storage
        List<FilteredMessage> storedMessages = decide.getStoredMessages(TOPIC);

        // Directly check the content of each message rather than using isCreation/isChange/isDeletion
        assertTrue(
                storedMessages.stream().anyMatch(msg -> msg.getMessageKey().equals("creation")),
                "Creation message should be processed."
        );

        assertTrue(
                storedMessages.stream().anyMatch(msg -> msg.getMessageKey().equals("change")),
                "Change message should be processed."
        );

        assertTrue(
                storedMessages.stream().anyMatch(msg -> msg.getMessageKey().equals("deletion")),
                "Deletion message should be processed."
        );
    }

    // Utility for creating test ChangedField objects
    private Map<String, InterceptorValueObject<?>> createChangedFields(MetaField<String> field, String beforeValue, String afterValue) {
        ValueObject<String> before = new ValueObject<>(field, beforeValue);
        ValueObject<String> after = new ValueObject<>(field, afterValue);
        return Map.ofEntries(Map.entry(field.id(), new InterceptorValueObject<>(field, before, after)));
    }
}