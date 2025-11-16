package com.aicon.tos.connect;

import com.aicon.tos.shared.AiconTosMediatorConfig;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AiconTosMediatorConfigTest {

    @Test
    void testConstants() {
        // Test that the constants have the expected values
        assertEquals("REQ_INDEX_444444", AiconTosMediatorConfig.REQUEST_ID);
        assertEquals(2, AiconTosMediatorConfig.NUMBER_OF_DIFFS);
        assertEquals(2, AiconTosMediatorConfig.CORRECT_LENGTH_AFTER_SPLIT);
    }

    @Test
    void testPrivateConstructor() {
        // Use reflection to test the private constructor
        assertThrows(IllegalStateException.class, () -> {
            var constructor = AiconTosMediatorConfig.class.getDeclaredConstructor();
            constructor.setAccessible(true);
            try {
                constructor.newInstance();
            } catch (InvocationTargetException e) {
                // Unwrap the cause of the InvocationTargetException
                throw (IllegalStateException) e.getCause();
            }
        }, "Utility class constructor should throw IllegalStateException");
    }
}