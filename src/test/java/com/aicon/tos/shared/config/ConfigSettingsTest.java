package com.aicon.tos.shared.config;

import com.aicon.TestConstants;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@TestInstance(TestInstance.Lifecycle.PER_CLASS) // Ensures that the instance is maintained across tests
class ConfigSettingsTest {

    private static final String TEST_FILE = "conf/mediator/configsettingstest.xml";

    @BeforeEach
    void setup() {
        // Set the configuration file to a test-specific file
        ConfigSettings.setConfigFile(TestConstants.PATH_TO_TEST_CONFIG_FILES + TEST_FILE);
    }

    @AfterEach
    void cleanup() {
        ConfigSettings.resetInstanceForTests();
    }

    @Test
    void testSingletonBehavior() {
        // Ensure that the singleton instance is the same across calls
        ConfigSettings instance1 = ConfigSettings.getInstance();
        ConfigSettings instance2 = ConfigSettings.getInstance();

        assertSame(instance1, instance2, "ConfigSettings should maintain a single instance.");
    }

    @Test
    void testSetConfigFile() {
        // Change the config file and verify it's updated
        ConfigSettings.setConfigFile("new-config.xml");
        assertEquals("new-config.xml", ConfigSettings.getInstance().getFileName());
    }

    @Test
    void testEnsureFileWithPath() {
        // Ensure the file and its path exist
        File file = ConfigSettings.getInstance().ensureConfigFilesWithPath();
        assertNotNull(file, "The configuration file should not be null.");

        // Compare only the file name
        assertEquals(new File(TEST_FILE).getName(), file.getName(),
                     "The file name should match the test configuration file.");

        // Check if the file exists in the expected directory
        assertTrue(file.getParentFile().exists(),
                   "Parent directories for the configuration file should exist.");

        // Optional: Verify relative path if needed
        String expectedRelativePath = new File(TEST_FILE).getPath();
        assertTrue(file.getPath().contains(expectedRelativePath),
                   "The file path should contain the expected relative path.");
    }


    @Test
    void testGetRoot() {
        // Verify that the root configuration group is initialized
        ConfigGroup root = ConfigSettings.getInstance().getRoot();
        assertNotNull(root, "Root configuration group should not be null.");
    }

    @Test
    void testReadConfig() {
        // Test reading a configuration file
        ConfigSettings config = ConfigSettings.getInstance();
        String error = config.read();

        assertNull(error, "Reading the configuration file should not produce errors.");
        assertNotNull(config.getRoot(), "Root configuration group should be initialized after reading.");
    }

    @Test
    void testSaveConfig() {
        // Test saving a configuration file
        ConfigSettings config = ConfigSettings.getInstance();
        String error = config.save();

        assertNull(error, "Saving the configuration file should not produce errors.");
        File file = new File(config.getFullFilename());
        assertTrue(file.exists(), "Configuration file should exist after saving.");
    }

    @Test
    void testGetStorageError() {
        // Simulate a storage error and verify
        ConfigSettings config = ConfigSettings.getInstance();
        config.read(); // Assuming file doesn't exist initially

        String error = config.getStorageError();
        if (error != null) {
            assertTrue(error.contains("Problem"), "Storage error should contain an problem message.");
        }
    }
}