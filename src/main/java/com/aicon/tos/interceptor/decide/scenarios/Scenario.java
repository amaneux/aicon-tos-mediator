package com.aicon.tos.interceptor.decide.scenarios;

import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.InterceptorEntityConfig;
import com.aicon.tos.interceptor.InterceptorValueObject;
import com.avlino.common.ValueObject;
import org.slf4j.Logger;

import java.util.LinkedList;
import java.util.concurrent.ConcurrentMap;

/**
 * Interface for defining processing or correlation scenarios.
 */
public interface Scenario {
    static final String DEFAULT_USER_ID = "CDC_interceptor";
    static final String UNIQUE_ID_PFX   = "ACI-";     // Aicon CDC Interceptor

    /**
     * Will be called when setting up this scenario.
     * @param name the name of the scenario
     * @param config the name of the entity this scenario is linked to.
     */
    void init(
            String name,
            InterceptorEntityConfig config
    );

    boolean isRunning();

    /**
     * @return true when it adds information to the MessageMeta of the message Defaults to true).
     */
    boolean addsMessageMeta();

    /**
     * @return the name for this scenario
     */
    String getName();

    /**
     * @return the entity name this scenario belongs to
     */
    String getEntityName();

    /**
     * Gets called to check if this event is relevant for the scenario. Basically it verifies if it meets the
     * fieldChanges and fieldActions list, but can be super seeded with more specific validations if needed.
     * When true this message will be stored for later reference and decide will call this method {@link #processMessage(FilteredMessage, ConcurrentMap)}.
     * @param event the event to validate
     * @return true when event is relevant to process (and stored).
     */
    boolean isRelevantEvent(CollectedMessage event);

    /**
     * Processes a new incoming filtered message in its own thread (arranged by the caller),
     * with access to all stored messages.
     * Note: be careful to synchronize on <code>globalMessageStore</code> when processing is taken longer then a few ms
     * because it will block further reading of the pipelines.
     *
     * @param newMessage           The new filtered message to process.
     * @param globalMessageStorage The shared storage containing all messages grouped by entity.
     */
    void processMessage(FilteredMessage newMessage, ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage);

    /**
     * Stops the scenario processing, e.g., to clean up resources.
     */
    void stop();

    default boolean isMatchingField(
            InterceptorValueObject<?> field,  // The field being checked
            String fieldName,                 // The field name to match
            Object referenceValue            // The reference value to compare
    ) {
        if (!fieldName.equals(field.field().id())) {
            return false; // Skip fields that don't match the field name
        }

        try {
            // Create a ValueObject and retrieve the actual, typed field value
            ValueObject<?> vo = new ValueObject<>(field.getMetaField(), field.afterValue());
            Object fieldValue = vo.value(); // Properly typed value from the field

            if (fieldValue == null || referenceValue == null) {
                return false;
            }

            // Convert and compare the values if types don't match
            if (!fieldValue.getClass().equals(referenceValue.getClass())) {
                return compareDifferentTypes(fieldValue, referenceValue);
            }

            // Direct comparison for matching types
            return fieldValue.equals(referenceValue);
        } catch (Exception e) {
            getLogger().error("Error processing field '{}', value: {}, referenceValue: {}, error: {}",
                    field.field().id(), field.afterValue(), referenceValue, e.getMessage());
            return false;
        }
    }

    private boolean compareDifferentTypes(Object fieldValue, Object referenceValue) {
        try {
            if (fieldValue instanceof Number && referenceValue instanceof String) {
                // Convert String to Number (e.g., "12345" -> 12345L)
                return fieldValue.equals(Long.valueOf(referenceValue.toString()));
            } else if (fieldValue instanceof String && referenceValue instanceof Number) {
                // Convert Number to String (e.g., 12345L -> "12345")
                return fieldValue.toString().equals(referenceValue.toString());
            }
        } catch (NumberFormatException e) {
            getLogger().error("Failed to convert values for comparison: fieldValue={}, referenceValue={}, error={}",
                    fieldValue, referenceValue, e.getMessage());
        }
        return false; // Return false if conversion fails or types are incompatible
    }


    // Force implementing classes to provide a logger
    Logger getLogger();

}