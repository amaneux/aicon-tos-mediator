package com.aicon.tos.shared.config;

import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import javax.xml.XMLConstants;
import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConfigGroupTest {

    @Test
    public void testReorderAttributes() {
        // Arrange: Create a sample XML with incorrect attribute order
        Element root = new Element("Flow");
        Element child = new Element("ConfigItem")
                .setAttribute("value", "")
                .setAttribute("key", "url.path")
                .setAttribute("comment", "Extends the path of the connection URL");
        root.addContent(child);

        System.out.println("Before reordering:");
        System.out.println(new XMLOutputter(Format.getPrettyFormat()).outputString(root));

        // Act: Reorder attributes
        ConfigGroup configGroup = new ConfigGroup(ConfigType.CanaryCheck, "testFlow");
        configGroup.reorderAttributes(root);

        System.out.println("After reordering:");
        System.out.println(new XMLOutputter(Format.getPrettyFormat()).outputString(root));

        // Assert: Check correct order
        List<Attribute> attributes = root.getChild("ConfigItem").getAttributes();
        assertEquals("key", attributes.get(0).getName());
        assertEquals("value", attributes.get(1).getName());
        assertEquals("comment", attributes.get(2).getName());
    }

    @Test
    public void testFromXml_WithValidData() {
        Element root = new Element("Config");
        Element childGroup = new Element("Flows")
                .setAttribute("name", "TestFlows");
        Element childItem = new Element("ConfigItem")
                .setAttribute("key", "example.key")
                .setAttribute("value", "example value");
        childGroup.addContent(childItem);
        root.addContent(childGroup);

        ConfigGroup group = new ConfigGroup(ConfigType.Config);
        group.fromXml(root);

        assertNotNull(group);
        assertEquals("TestFlows", group.getChildGroup(ConfigType.Flows).getName());
        assertEquals("example value",
                group.getChildGroup(ConfigType.Flows).findItem("example.key").value());
    }

    @Test
    public void testFromXml_WithInvalidConfigType() {
        Element root = new Element("InvalidType");
        ConfigGroup group = new ConfigGroup(ConfigType.Config);

        assertThrows(IllegalArgumentException.class, () -> group.fromXml(root));
    }

    @Test
    public void testFromXml_ParseWholeXmlFile() throws Exception {
        File xmlFile = new File("src/test/resources/conf/mediator/configgrouptest.xml");
        assertTrue(xmlFile.exists(), "XML file does not exist: " + xmlFile.getAbsolutePath());

        SAXBuilder saxBuilder = new SAXBuilder();
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        saxBuilder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");

        Document document = saxBuilder.build(xmlFile);
        Element rootElement = document.getRootElement();

        ConfigGroup configGroup = new ConfigGroup(ConfigType.valueOf(rootElement.getName()));
        configGroup.fromXml(rootElement);

        assertNotNull(configGroup);
        assertEquals(ConfigType.Config, configGroup.getType());
        assertFalse(configGroup.getChildren().isEmpty(), "ConfigGroup should have child groups");

        // Log de hele ConfigGroup-structuur voor validatie
        System.out.println(configGroup.toString());
    }
}

