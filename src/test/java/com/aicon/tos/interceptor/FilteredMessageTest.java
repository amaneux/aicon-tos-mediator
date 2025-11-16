package com.aicon.tos.interceptor;

import com.avlino.common.MetaField;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.aicon.tos.connect.cdc.CDCAction.CHANGED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class FilteredMessageTest {

    @Test
    void getChangedFields() {
        // Arrange: Create a FilteredMessage instance with mocked data
        List<InterceptorValueObject<?>> changedFields = Collections.emptyList();
        CollectedMessage collectedMessage = new CollectedMessage(CHANGED, "TestEntity", 0, 0, "TestKey",
                Map.ofEntries());
        FilteredMessage filteredMessage = new FilteredMessage(collectedMessage);

        // Act: Call the method
        var result = filteredMessage.getChangedFields();

        // Assert: Validate the result is not null
        assertNotNull(result, "Changed fields should not be null");
    }

    @Test
    void getPayload() {
        // Arrange: Create a FilteredMessage instance with mocked data
        List<InterceptorValueObject<?>> changedFields = Collections.emptyList();
        CollectedMessage collectedMessage = new CollectedMessage(CHANGED, "TestEntity", 0, 0, "TestKey",
                Map.ofEntries());
        FilteredMessage filteredMessage = new FilteredMessage(collectedMessage);

        // Act: Get the payload
        var result = filteredMessage.getPayloadAsJson();

        // Assert: Validate the payload
        assertNotNull(result, "Payload should not be null");
    }

    @Test
    void getFieldValueByNameAsString() {
        // Arrange: Create a FilteredMessage instance with mocked data
        Map<String, InterceptorValueObject<?>> fields = Collections.emptyMap();
        CollectedMessage collectedMessage = new CollectedMessage(CHANGED, "TestEntity", 0, 0, "TestKey", fields);
        FilteredMessage filteredMessage = new FilteredMessage(collectedMessage);

        // Act: Call the getFieldValueByName method and test different field names
        var existingField = filteredMessage.getFieldValueAsString("existingField");
        var nonExistingField = filteredMessage.getFieldValueAsString("nonExistingField");

        // Assert: Test results for fields
        assertNull(existingField, "Field value for 'existingField' should be null");
        assertNull(nonExistingField, "Field value for 'nonExistingField' should be null");
    }


    @Test
    void testToString() {
        // Arrange: Create a FilteredMessage instance with mocked data
        Map<String, InterceptorValueObject<?>> fields = Collections.emptyMap();
        CollectedMessage collectedMessage = new CollectedMessage(CHANGED, "TestEntity", 0, 0, "TestKey", fields);
        FilteredMessage filteredMessage = new FilteredMessage(collectedMessage);

        // Act: Call the toString method
        var result = filteredMessage.toString();

        // Assert: Test that the result is not null
        assertNotNull(result, "toString output should not be null");
    }

    @Test
    void testGetFieldValueByName_FixesOriginalProblem() {
        // Arrange: Fields list with problematic field data
        Map<String, InterceptorValueObject<?>> fields = Map.ofEntries(
                Map.entry("truck_visit_ref", new InterceptorValueObject<>(new MetaField<>("truck_visit_ref", String.class), "beforeValue", null)), // `truck_visit_ref` exists, but `afterValue` is null
                Map.entry("some_other_field", new InterceptorValueObject<>(new MetaField<>("some_other_field", String.class), "beforeValue", "correct_value")), // Normal case
                Map.entry("null", new InterceptorValueObject<>(null, null, null)) // Completely null field
        );

        CollectedMessage collectedMessage = new CollectedMessage(CHANGED, "TestEntity", 0, 0, "TestKey", fields);
        FilteredMessage filteredMessage = new FilteredMessage(collectedMessage);

        // Act & Assert: Field exists with null afterValue
        String result1 = filteredMessage.getFieldValueAsString("truck_visit_ref");
        assertEquals(result1, null, "Expected null due to null afterValue for truck_visit_ref.");

        // Act & Assert: Field exists with valid data
        String result2 = filteredMessage.getFieldValueAsString("some_other_field");
        assertEquals("correct_value", result2, "Expected correct value for some_other_field.");

        // Act & Assert: Non-existent fieldName
        String result3 = filteredMessage.getFieldValueAsString("non_existent_field");
        assertNull(result3, "Expected null because no match exists for non_existent_field.");

        // Act & Assert: Null input as fieldName
        String result4 = filteredMessage.getFieldValueAsString(null);
        assertNull(result4, "Expected null because fieldName is null.");
    }
}