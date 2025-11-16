package com.aicon.tos.interceptor.decide;

import com.aicon.tos.interceptor.FilteredMessage;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;

/**
 * Utility class for querying stored FilteredMessages.
 */
public class StoredMessageSearcher {

    private final ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage;

    /**
     * Constructor accepting the global message storage.
     *
     * @param globalMessageStorage A map where the key is the topic name,
     *                       and the value is the list of messages for that topic.
     */
    public StoredMessageSearcher(ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage) {
        this.globalMessageStorage = globalMessageStorage;
    }

    /**
     * Retrieves all messages for a specific topic.
     *
     * @param topic The topic name.
     * @return An immutable list of messages for the topic, or an empty list if none found.
     */
    public List<FilteredMessage> findMessagesByTopic(String topic) {
        return getMessagesOrEmpty(topic);
    }

    /**
     * Retrieves all messages that match a specified field name and value within a topic.
     *
     * @param topic The topic name.
     * @param fieldName   The name of the field to match.
     * @param fieldValue The value to match. If null, matches any value for the given field.
     * @return A list of matching messages.
     */
    public List<FilteredMessage> findMessagesByField(String topic, String fieldName, Object fieldValue) {
        return getMessagesOrEmpty(topic).stream()
                .filter(message -> message.getFields().stream()
                        .anyMatch(field -> field.field().id().equals(fieldName) &&
                                (fieldValue == null || field.afterValue().equals(fieldValue)))
                )
                .toList();
    }

    /**
     * Finds messages in a topic that satisfy a custom condition.
     *
     * @param topic     The topic name.
     * @param predicate The custom condition to match messages.
     * @return A list of messages that match the predicate.
     */
    public List<FilteredMessage> findMessagesByCondition(String topic, Predicate<FilteredMessage> predicate) {
        return getMessagesOrEmpty(topic).stream()
                .filter(predicate)
                .toList();
    }

    /**
     * Searches messages across all topics using a custom condition.
     *
     * @param predicate The condition to match messages across all topics.
     * @return A list of messages that match the specified condition across all topics.
     */
    public List<FilteredMessage> searchMessagesAcrossTopics(Predicate<FilteredMessage> predicate) {
        return globalMessageStorage.values().stream()
                .flatMap(List::stream)
                .filter(predicate)
                .toList();
    }

    /**
     * Utility method to retrieve messages for a topic or return an empty list if none exist.
     *
     * @param topic The topic name.
     * @return A list of messages for the topic, or an empty list if the topic does not exist.
     */
    private List<FilteredMessage> getMessagesOrEmpty(String topic) {
        LinkedList<FilteredMessage> messages = globalMessageStorage.get(topic);
        return (messages != null) ? new LinkedList<>(messages) : Collections.emptyList();
    }
}