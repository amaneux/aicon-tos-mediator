package com.aicon.tos.shared.xml;

import generated.Config;
import generated.Connections;
import generated.Http;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import jakarta.xml.bind.Unmarshaller;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.input.sax.XMLReaders;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class XMLHandler {

    private static final Logger LOG = LoggerFactory.getLogger(XMLHandler.class);

    public static <T> T readXML(String filePath, Class<T> clazz) {
        try {
            JAXBContext context = JAXBContext.newInstance(clazz);

            Unmarshaller unmarshaller = context.createUnmarshaller();

            @SuppressWarnings("unchecked")
            T object = (T) unmarshaller.unmarshal(new File(filePath));
            return object;

        } catch (JAXBException e) {
            e.printStackTrace();
            return null;
        }
    }


    public Optional<Http> findHttpConfigByRef(Connections connections, String httpRef) {
        if (connections == null || connections.getKafkaOrHttp() == null) {
            return Optional.empty();
        }

        return connections.getKafkaOrHttp().stream()
                .filter(Http.class::isInstance)
                .map(Http.class::cast)
                .filter(http -> httpRef.equals(http.getName()))
                .findFirst();
    }

    public static boolean writeXML(String filePath, Config config) {
        try {
            // Convert Config to a JDOM Element
            Element rootElement = convertToElement(config);
            if (rootElement == null) { return false;}

            // Detach root element if it belongs to an existing document
            rootElement = rootElement.detach();

            // Reorder attributes
            reorderAttributes(rootElement);

            // Write the reordered XML to the file
            Document document = new Document(rootElement);
            XMLOutputter outputter = new XMLOutputter(Format.getPrettyFormat());
            outputter.output(document, new FileWriter(filePath));

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public static void reorderAttributes(Element element) {
        List<Attribute> attributes = new ArrayList<>(element.getAttributes());

        // Define the desired order of attributes
        List<String> order = List.of("key", "value", "comment");

        // Sort attributes according to the desired order
        attributes.sort((a1, a2) -> {
            int index1 = order.indexOf(a1.getName());
            int index2 = order.indexOf(a2.getName());
            return Integer.compare(index1, index2);
        });

        // Clear and re-add the attributes in the correct order
        element.getAttributes().clear(); // Clear existing attributes
        for (Attribute attr : attributes) {
            element.setAttribute(attr); // Add each attribute back in the sorted order
        }

        // Recursively reorder child elements
        for (Element child : element.getChildren()) {
            reorderAttributes(child);
        }
    }


    public static Element convertToElement(Config config) {
        try {
            // Marshal the Config object to an XML string
            JAXBContext jaxbContext = JAXBContext.newInstance(Config.class);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, false);

            StringWriter sw = new StringWriter();
            marshaller.marshal(config, sw);
            String xmlString = sw.toString();

            // Parse the XML string into a JDOM Document
            // Use the SAXBuilder configured to prevent XXE attacks
            SAXBuilder saxBuilder = new SAXBuilder(XMLReaders.NONVALIDATING);
            saxBuilder.setProperty("http://javax.xml.XMLConstants/property/accessExternalDTD", "");
            saxBuilder.setProperty("http://javax.xml.XMLConstants/property/accessExternalSchema", "");
            Document jdomDoc = saxBuilder.build(new StringReader(xmlString));

            // Get the root element
            return jdomDoc.getRootElement();
        } catch (Exception e) {
            e.printStackTrace();
            // Log the error with additional context
            LOG.error("Failed to convert Config object to XML Element: {}", e.getMessage(), e);

            // Optional: Throw a custom exception or return null, depending on app needs
            throw new RuntimeException("Error while converting Config object to XML Element", e);
        }
    }
}
