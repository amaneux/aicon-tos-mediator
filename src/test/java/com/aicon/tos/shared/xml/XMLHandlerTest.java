package com.aicon.tos.shared.xml;

import generated.Config;
import generated.ConfigItem;
import generated.ConfigItemKeyEnum;
import generated.Connections;
import generated.Http;
import generated.Kafka;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

public class XMLHandlerTest {

    private static final String TEST_DIRECTORY = "src/test/resources/conf/mediator/";
    private static final String originalFilePath =
            Paths.get(TEST_DIRECTORY + "aicon-connections.xml").toString();
    private static final String updatedFilePath =
            Paths.get(TEST_DIRECTORY + "updated-aicon-connections.xml").toString();

    /**
     * Tests reading XML into a Config object, verifying the essential structure.
     */
    @Test
    public void testReadXML() {
        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");
        assertNotNull(config.getTosControl().getCanaryCheck().getHttpRef(), "Referred http should not be null");
        assertNotNull(config.getConnections(), "Connections should not be null");
    }

    /**
     * Tests adding a new Http configuration element to the Config object.
     */
    @Test
    public void testAddHttpConfig() {
        Http newHttp = new Http();
        newHttp.setName("NEW-REF");

        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");

        config.getConnections().getKafkaOrHttp().add(newHttp);
        assertTrue(config.getConnections().getKafkaOrHttp().contains(newHttp),
                   "New Http config should be added to Connections");
    }

    /**
     * Tests adding a new Kafka configuration element to the Config object.
     */
    @Test
    public void testAddKafkaConfig() {
        ConfigItem configItem1 = new ConfigItem();
        configItem1.setKey(ConfigItemKeyEnum.HOSTNAME);
        configItem1.setValue("kafka.example.com");

        ConfigItem configItem2 = new ConfigItem();
        configItem2.setKey(ConfigItemKeyEnum.HOSTPORT);
        configItem2.setValue("9092");

        Kafka newKafka = new Kafka();
        newKafka.setName("NewKafkaConfig");
        newKafka.getConfigItem().add(configItem1);
        newKafka.getConfigItem().add(configItem2);

        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");

        config.getConnections().getKafkaOrHttp().add(newKafka);
        assertTrue(config.getConnections().getKafkaOrHttp().contains(newKafka),
                   "New Kafka config should be added to Connections");
    }

    /**
     * Tests writing the Config object to XML to ensure the output file is created and updated correctly.
     */
    @Test
    public void testWriteXMLWithReorder() {
        // Step 1: Read the original XML
        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");

        // Step 2: Write the updated Config object with reordered attributes
        boolean isWritten = XMLHandler.writeXML(updatedFilePath, config);
        assertTrue(isWritten, "XML should be successfully written to the file");

        // Step 3: Read the updated XML into Config object
        Config updatedConfig = XMLHandler.readXML(updatedFilePath, Config.class);
        assertNotNull(updatedConfig, "Updated config object should not be null");
    }

    @Test
    public void testWriteXMLAttributeOrder() {
        // Step 1: Read the original XML
        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");

        String filePath = TEST_DIRECTORY + "test-output.xml";

        // Step 2: Write the Config to XML
        boolean isWritten = XMLHandler.writeXML(filePath, config);
        assertTrue(isWritten, "XML should be successfully written to the file");

        // Step 3: Read the written file and verify attribute order
        try {
            SAXBuilder saxBuilder = new SAXBuilder();
            Document document = saxBuilder.build(new File(filePath));
            Element rootElement = document.getRootElement();

            // Zoek naar alle ConfigItem-elementen
            List<Element> configItems = findElementsByName(rootElement, "ConfigItem");
            assertFalse(configItems.isEmpty(), "ConfigItem elements should exist in the XML");

            for (Element configItemElement : configItems) {
                // Get the attributes and verify their order
                List<Attribute> attributes = configItemElement.getAttributes();
                assertEquals("key", attributes.get(0).getName());
                assertEquals("value", attributes.get(1).getName());
                if (attributes.size()>2) {
                    assertEquals("comment", attributes.get(2).getName());
                }
            }

            // Print the verified output
            System.out.println("Verified XML:\n" + new XMLOutputter(Format.getPrettyFormat()).outputString(document));
        } catch (Exception e) {
            fail("Exception occurred while reading the XML file: " + e.getMessage());
        }
    }

    private List<Element> findElementsByName(Element root, String name) {
        List<Element> elements = new ArrayList<>();
        if (root.getName().equals(name)) {
            elements.add(root);
        }
        for (Element child : root.getChildren()) {
            elements.addAll(findElementsByName(child, name));
        }
        return elements;
    }


    /**
     * Tests adding a new Http configuration to Config, writing it to XML, and verifying it is persisted.
     */
    @Test
    public void testAddAndVerifyNewHttpConfig() {
        // Step 1: Read the original XML into Config
        Config config = XMLHandler.readXML(originalFilePath, Config.class);
        assertNotNull(config, "Config object should not be null");

        // Step 2: Add a new Http configuration with NEW-REF reference
        ConfigItem configItem1 = new ConfigItem();
        configItem1.setKey(ConfigItemKeyEnum.HOSTNAME);
        configItem1.setValue("kafka.example.com");

        ConfigItem configItem2 = new ConfigItem();
        configItem2.setKey(ConfigItemKeyEnum.HOSTPORT);
        configItem2.setValue("9092");

        Kafka newKafka = new Kafka();
        newKafka.setName("NewKafkaConfig");
        newKafka.getConfigItem().add(configItem1);

        Http newHttp = new Http();
        newHttp.setName("NEW-REF");
        newHttp.getConfigItem().add(configItem2);

        // Ensure Connections exists, then add Http and Kafka to it
        if (config.getConnections() == null) {
            config.setConnections(new Connections());
        }
        config.getConnections().getKafkaOrHttp().add(newHttp);
        config.getConnections().getKafkaOrHttp().add(newKafka);

        // Step 3: Write the updated Config object to XML
        boolean isWritten = XMLHandler.writeXML(updatedFilePath, config);
        assertTrue(isWritten, "XML should be successfully written to the file");

        // Step 4: Read the updated XML and verify the new Http configuration is present
        Config updatedConfig = XMLHandler.readXML(updatedFilePath, Config.class);
        assertNotNull(updatedConfig, "Updated config object should not be null");

        Optional<Http> newHttpConfig = updatedConfig.getConnections().getKafkaOrHttp().stream()
                .filter(item -> item instanceof Http)
                .map(Http.class::cast)
                .filter(http -> "NEW-REF".equals(http.getName()))
                .findFirst();

        assertTrue(newHttpConfig.isPresent(),
                   "Newly added Http config with NEW-REF should be present in the updated XML");
    }

    @Test
    public void testReorderAttributes() {
        Element element = new Element("ConfigItem");
        element.setAttribute("value", "value1");
        element.setAttribute("key", "key1");
        element.setAttribute("comment", "comment1");

        XMLHandler.reorderAttributes(element);

        List<Attribute> attributes = element.getAttributes();
        assertEquals("key", attributes.get(0).getName());
        assertEquals("value", attributes.get(1).getName());
        assertEquals("comment", attributes.get(2).getName());
    }

}
