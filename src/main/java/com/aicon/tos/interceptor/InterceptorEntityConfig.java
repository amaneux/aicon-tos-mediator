package com.aicon.tos.interceptor;

import com.aicon.tos.shared.config.ConfigGroup;

import java.time.Duration;


/**
 * This class represents the configuration for an interceptor topic, defining its storage
 * properties, processing delays, and testing behavior for specific operations such as creations,
 * changes, and deletions.
 *
 * The configuration is immutable and is typically used to configure topics in a system designed
 * for handling intercepted messages. It allows specifying constraints on message storage and
 * defines fields to compare for detecting changes in the intercepted messages.
 *
 * Fields:
 * - `topicName`: Identifies the topic for which the configuration applies.
 * - `maxNrMessagesInStorage`: Limits the maximum number of messages that can be stored for the topic.
 * - `maxTimeInStorage`: Defines the maximum duration messages can remain in storage.
 * - `processingDelay`: Specifies the delay in processing messages, in milliseconds.
 * - `compareFields`: A list of fields that are used to compare messages and detect changes.
 * - `testCreations`: Indicates if the configuration should test for message creation events.
 * - `testChanges`: Indicates if the configuration should test for message change events.
 * - `testDeletions`: Indicates if the configuration should test for message deletion events.
 *
 * Methods:
 * - Getters are provided for each field to access the configuration details.
 * - `getTopicName()`: Retrieves the name of the topic.
 * - `getMaxNrMessagesInStorage()`: Returns the maximum number of messages allowed in storage.
 * - `getMaxTimeInStorage()`: Retrieves the maximum duration for storing messages.
 * - `getProcessingDelay()`: Gets the message processing delay in milliseconds.
 * - `getCompareFields()`: Accesses the list of fields used for message comparison.
 * - `isTestCreations()`: Indicates whether creation tests are enabled.
 * - `isTestChanges()`: Indicates whether change tests are enabled.
 * - `isTestDeletions()`: Indicates whether deletion tests are enabled.
 */
public class InterceptorEntityConfig {
    private final String entityName;
    private final String topicName;
    private final String groupId;
    private final int maxNrMessagesInStorage;
    private final Duration maxTimeInStorage;
    private final long processingDelay;
    private final ConfigGroup scenariosGroup;
    private final boolean testCreations;
    private final boolean testChanges;
    private final boolean testDeletions;
    private boolean useMockedInterceptor = false;

    public InterceptorEntityConfig(String entityName, String topicName, String groupId, int maxNrMessagesInStorage, Duration maxTimeInStorage,
                                   long processingDelay, ConfigGroup scnGrp,
                                   boolean testCreations, boolean testChanges, boolean testDeletions) {
        this.entityName = entityName;
        this.topicName = topicName;
        this.groupId = groupId;
        this.maxNrMessagesInStorage = maxNrMessagesInStorage;
        this.maxTimeInStorage = maxTimeInStorage;
        this.processingDelay = processingDelay;
        this.scenariosGroup = scnGrp;
        this.testCreations = testCreations;
        this.testChanges = testChanges;
        this.testDeletions = testDeletions;
    }

    // Getters
    public String getEntityName() {
        return entityName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getGroupId() {
        return groupId;
    }

    public int getMaxNrMessagesInStorage() {
        return maxNrMessagesInStorage;
    }

    public Duration getMaxTimeInStorage() {
        return maxTimeInStorage;
    }

    public long getProcessingDelay() {
        return processingDelay;
    }

    public ConfigGroup getScenariosGroup() {return scenariosGroup;}

    public boolean isTestCreations() {
        return testCreations;
    }

    public boolean isTestChanges() {
        return testChanges;
    }

    public boolean isTestDeletions() {
        return testDeletions;
    }

    public void setUseMockedInterceptor(boolean useMockedConsumers) {
        this.useMockedInterceptor = useMockedConsumers;
    }

    public boolean useMockedInterceptor() {
        return useMockedInterceptor;
    }
}
