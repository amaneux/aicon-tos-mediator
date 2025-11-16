package com.aicon.tos;

import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigDomainTest {

    private ConfigSettings configSettingsMock;
    private ConfigGroup configGroupMock;

    @BeforeEach
    void setUp() {
        configSettingsMock = mock(ConfigSettings.class);
        ConfigSettings.setMockInstance(configSettingsMock); // Mock the ConfigSettings singleton

        configGroupMock = mock(ConfigGroup.class);

        // Reset the static `topicPrefix` before each test
        ConfigDomain.resetTopicPrefixForTests();
    }

    @AfterEach
    void tearDown() {
        ConfigSettings.resetInstanceForTests(); // Reset the singleton for consistent state
    }

    @Test
    void getScopePart() {
        // Arrange
        String mockScopeValue = "operator1/complex1/facility1/yard1";

        ConfigGroup generalGroupMock = mock(ConfigGroup.class); // For ConfigType.General
        ConfigGroup generalItemsGroupMock = mock(ConfigGroup.class); // For ConfigType.GeneralItems
        ConfigItem yardScopeItemMock = mock(ConfigItem.class); // For the CFG_YARD_SCOPE key

        // Mock getInstance and ConfigGroup calls
        when(configSettingsMock.getMainGroup(ConfigType.General)).thenReturn(generalGroupMock);
        when(generalGroupMock.getChildGroup(ConfigType.GeneralItems)).thenReturn(generalItemsGroupMock);
        when(generalItemsGroupMock.findItem(ConfigDomain.CFG_YARD_SCOPE)).thenReturn(yardScopeItemMock);
        when(yardScopeItemMock.value()).thenReturn(mockScopeValue);

        // Assert
        assertEquals("operator1", ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_OPERATOR));
        assertEquals("complex1", ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_COMPLEX));
        assertEquals("facility1", ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_FACILITY));
        assertEquals("yard1", ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_YARD));
    }


    @Test
    void getGeneralItem() {
        // Arrange
        String itemKey = "test.item";
        String expectedValue = "expectedValue";

        // Mock the chain of calls
        ConfigGroup generalGroupMock = mock(ConfigGroup.class); // For ConfigType.General
        ConfigGroup generalItemsGroupMock = mock(ConfigGroup.class); // For ConfigType.GeneralItems
        ConfigItem configItemMock = mock(ConfigItem.class); // For the actual item

        when(configSettingsMock.getMainGroup(ConfigType.General)).thenReturn(generalGroupMock);
        when(generalGroupMock.getChildGroup(ConfigType.GeneralItems)).thenReturn(generalItemsGroupMock);
        when(generalItemsGroupMock.findItem(itemKey)).thenReturn(configItemMock);
        when(configItemMock.value()).thenReturn(expectedValue);

        // Act
        String actualValue = ConfigDomain.getGeneralItem(itemKey);

        // Assert
        assertEquals(expectedValue, actualValue);
    }

    @Test
    void getPrefixedTopic() {
        // Arrange
        String itemName = "itemName";
        String topicName = "testTopic";
        when(configGroupMock.getItemValue(itemName)).thenReturn(topicName);

        // Act
        String result = ConfigDomain.getPrefixedTopic(configGroupMock, itemName);

        // Assert
        assertEquals(ConfigDomain.prefixIfNeeded(topicName), result);
    }

    @Test
    void prefixIfNeeded() {
        // Arrange
        String validTopic = "prefix.valid.topic";
        String invalidTopic = "topic";
        String expectedPrefix = "cdc.prefix.";

        ConfigGroup connectionsGroupMock = mock(ConfigGroup.class); // Mock for ConfigType.Connections group
        ConfigGroup kafkaGroupMock = mock(ConfigGroup.class);       // Mock for ConfigType.Kafka group

        when(configSettingsMock.getMainGroup(ConfigType.Connections)).thenReturn(connectionsGroupMock);
        when(connectionsGroupMock.getChildGroup(ConfigType.Kafka)).thenReturn(kafkaGroupMock);
        when(kafkaGroupMock.getItemValue(ConfigSettings.CFG_CDC_PREFIX)).thenReturn(expectedPrefix);

        // Act
        String prefixNotNeeded = ConfigDomain.prefixIfNeeded(validTopic);
        String prefixNeeded = ConfigDomain.prefixIfNeeded(invalidTopic);

        // Assert
        assertEquals(validTopic, prefixNotNeeded);
        assertTrue(prefixNeeded.startsWith(ConfigDomain.getTopicPrefix()));
    }

    @Test
    void getTopicPrefix() {
        // Arrange
        String expectedPrefix = "cdc.prefix.";

        ConfigGroup connectionsGroupMock = mock(ConfigGroup.class); // Mock for ConfigType.Connections group
        ConfigGroup kafkaGroupMock = mock(ConfigGroup.class);       // Mock for ConfigType.Kafka group

        when(configSettingsMock.getMainGroup(ConfigType.Connections)).thenReturn(connectionsGroupMock);
        when(connectionsGroupMock.getChildGroup(ConfigType.Kafka)).thenReturn(kafkaGroupMock);
        when(kafkaGroupMock.getItemValue(ConfigDomain.CFG_CDC_PREFIX)).thenReturn(expectedPrefix);

        // Act
        String prefix = ConfigDomain.getTopicPrefix();

        // Assert
        assertEquals(expectedPrefix, prefix);
    }
}