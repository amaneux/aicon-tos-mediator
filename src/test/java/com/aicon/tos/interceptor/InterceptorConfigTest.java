package com.aicon.tos.interceptor;

import com.aicon.TestConstants;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InterceptorConfigTest {

    private InterceptorConfig interceptorConfig;

    @BeforeEach
    void setUp() {
        ConfigSettings.setConfigFile(TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/interceptortest-connections.xml");
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup interceptorConfigGroup = config.getMainGroup(ConfigType.Interceptors);
        interceptorConfig = new InterceptorConfig(interceptorConfigGroup);
    }

    @Test
    void testEntitysAreLoadedCorrectly() {
        // Fetch entitys and validate them
        List<String> entitys = interceptorConfig.getEntityKeys();
        assertNotNull(entitys, "Entitys should not be null");
        assertEquals(2, entitys.size(), "There should be exactly 2 entitys loaded");
        assertTrue(entitys.contains("inv_wi (TEST)"), "Expected entity: inv_wi");
        assertTrue(entitys.contains("truck_transactions"), "Expected entity: AICON_TOS_CONTROL");
    }

    @Test
    void testEntityConfiguration() {
        // Fetch and validate configuration for a specific entity
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("inv_wi (TEST)");
        assertNotNull(config, "Configuration for entity 'inv_wi (TEST)' should not be null");

        assertEquals("inv_wi (TEST)", config.getEntityName(), "Entity name should match");
        assertEquals(10000, config.getMaxNrMessagesInStorage(), "Max messages for entity should be default");
        assertEquals(Duration.ofMinutes(30), config.getMaxTimeInStorage(), "Max time for entity should match default");
    }

    @Test
    void validateMockSetup() {
        // Controleer dat de geconfigureerde onderwerpen correct zijn geladen in InterceptorConfig
        List<String> loadedEntities = interceptorConfig.getEntityKeys();

        // Verwachte onderwerpen
        assertNotNull(loadedEntities, "Entities should not be null.");
        assertEquals(2, loadedEntities.size(), "Expected 2 entities to be loaded.");
        assertTrue(loadedEntities.contains("inv_wi (TEST)"), "Should contain entity: inv_wi (TEST).");
        assertTrue(loadedEntities.contains("truck_transactions"), "Should contain entity: truck_transactions.");
    }


    @Test
    void testGetEntitysReturnsConfiguredEntities() {
        // Act
        List<String> entities = interceptorConfig.getEntityKeys();

        // Assert
        assertNotNull(entities, "Entitys should not be null");
        assertEquals(2, entities.size(), "The number of configured entitys should be 2");
        assertTrue(entities.contains("inv_wi (TEST)"), "The entitys should include 'inv_wi'");
        assertTrue(entities.contains("truck_transactions"), "The entitys should include 'truck_transactions'");
    }

    @Test
    void testGetEntityConfigForValidEntity() {
        // Act
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("inv_wi (TEST)");

        // Assert
        assertNotNull(config, "Entity config should not be null");
        assertEquals("inv_wi (TEST)", config.getEntityName(), "Entity name should match 'inv_wi (TEST)'");
        assertEquals(10000, config.getMaxNrMessagesInStorage(), "Max messages in storage should match");
        assertEquals(Duration.ofMinutes(30), config.getMaxTimeInStorage(), "Max time in storage should match");
        assertEquals(2000L, config.getProcessingDelay(), "Processing delay should match");
        assertFalse(config.isTestCreations(), "Test creations flag should match");
        assertFalse(config.isTestChanges(), "Test changes flag should match");
        assertFalse(config.isTestDeletions(), "Test deletions flag should match");
    }

    @Test
    void testGetEntityConfigForEntityWithoutCompareFields() {
        // Act
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("truck_transactions");

        // Assert
        assertNotNull(config, "Entity config should not be null");
        assertEquals("truck_transactions", config.getEntityName(), "Entity name should match 'truck_transactions'");
        assertEquals(5000, config.getMaxNrMessagesInStorage(), "Max messages in storage should match");
        assertEquals(Duration.ofMinutes(30), config.getMaxTimeInStorage(), "Max time in storage should match");
        assertEquals(1500L, config.getProcessingDelay(), "Processing delay should match");
    }

    @Test
    void testGetEntityConfigForUnknownEntity() {
        // Act
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("unknownEntity");

        // Assert
        assertNull(config, "Config for an unknown entity should be null");
    }

    @Test
    void testGetCompareFieldsForUnknownEntity() {
        // Act
        InterceptorEntityConfig config = interceptorConfig.getEntityConfig("unknownEntity");

        // Assert
        assertNull(config, "config for an unknown entity should be null");
    }
}
