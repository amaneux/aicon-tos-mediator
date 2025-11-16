package com.aicon.tos.shared.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigTypeTest {

    @Test
    void testMatchesSameType() {
        // Test if a ConfigType matches itself
        assertTrue(ConfigType.Http.matches(ConfigType.Http), "Http should match itself");
        assertTrue(ConfigType.Kafka.matches(ConfigType.Kafka), "Kafka should match itself");
    }

    @Test
    void testMatchesRelatedTypes() {
        // Test if HttpRef matches Http and vice versa
        assertTrue(ConfigType.HttpRef.matches(ConfigType.Http), "HttpRef should match Http");
        assertTrue(ConfigType.Http.matches(ConfigType.HttpRef), "Http should match HttpRef");

        // Test if KafkaRef matches Kafka and vice versa
        assertTrue(ConfigType.KafkaRef.matches(ConfigType.Kafka), "KafkaRef should match Kafka");
        assertTrue(ConfigType.Kafka.matches(ConfigType.KafkaRef), "Kafka should match KafkaRef");
    }

    @Test
    void testDoesNotMatchUnrelatedTypes() {
        // Test unrelated types
        assertFalse(ConfigType.Http.matches(ConfigType.Kafka), "Http should not match Kafka");
        assertFalse(ConfigType.Kafka.matches(ConfigType.Http), "Kafka should not match Http");
        assertFalse(ConfigType.Http.matches(ConfigType.Flows), "Http should not match Flows");
        assertFalse(ConfigType.HttpRef.matches(ConfigType.Flow), "HttpRef should not match Flow");
    }

    @Test
    void testMatchReferenceType() {
        // Test matching from a set of types
        ConfigType[] connectionTypes = ConfigType.CONNECTION_TYPES;

        // HttpRef should match an element in CONNECTION_TYPES via Http
        assertTrue(matchReferenceType(connectionTypes, ConfigType.HttpRef), "HttpRef should match CONNECTION_TYPES via Http");
        assertTrue(matchReferenceType(connectionTypes, ConfigType.Http), "Http should match CONNECTION_TYPES");

        // KafkaRef should match an element in CONNECTION_TYPES via Kafka
        assertTrue(matchReferenceType(connectionTypes, ConfigType.KafkaRef), "KafkaRef should match CONNECTION_TYPES via Kafka");
        assertTrue(matchReferenceType(connectionTypes, ConfigType.Kafka), "Kafka should match CONNECTION_TYPES");

        // Flows should not match CONNECTION_TYPES
        assertFalse(matchReferenceType(connectionTypes, ConfigType.Flows), "Flows should not match CONNECTION_TYPES");
    }

    // Helper method for testing matchReferenceType logic
    private boolean matchReferenceType(ConfigType[] types, ConfigType matchType) {
        if (types == null || matchType == null) {
            return false;
        }

        for (ConfigType type : types) {
            if (type.matches(matchType)) {
                return true;
            }
        }
        return false;
    }
}
