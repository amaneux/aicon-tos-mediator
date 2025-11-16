package com.aicon.tos.shared.config;

import org.junit.jupiter.api.Test;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.fail;

public class XMLValidationTest {

    private static final String XSD_PATH = "src/test/resources/conf/mediator/schema/connect-configuration.xsd";
    private static final String TEST_XML_DIRECTORY = "src/test/resources/conf/mediator/";
    private static final String TEST_MAIN_CONFIG_FILE = "src/test/resources/conf/mediator/aicon-connections.xml";
    StringBuilder errorMessages = new StringBuilder();

    @Test
    void validateAllXMLFilesAgainstXSD() {
        File xsdFile = new File(XSD_PATH);
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

        try {
            Schema schema = schemaFactory.newSchema(xsdFile);
            Validator validator = schema.newValidator();

            List<File> xmlFiles = new ArrayList<>();
            xmlFiles.add(new File(TEST_MAIN_CONFIG_FILE));
            xmlFiles.addAll(getXMLFiles(new File(TEST_XML_DIRECTORY)));

            for (File xmlFile : xmlFiles) {
                validateFile(validator, xmlFile);
            }
        } catch (SAXException e) {
            fail("Schema loading failed: " + e.getMessage());
        }

        if (!errorMessages.isEmpty()) {
            fail("Validation errors found:\n" + errorMessages);
        }
    }

    private List<File> getXMLFiles(File directory) {
        File[] files = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        return files != null ? List.of(files) : new ArrayList<>();
    }

    private void validateFile(Validator validator, File xmlFile) {
        try {
            validator.validate(new StreamSource(xmlFile));
            System.out.println("Validated successfully: " + xmlFile.getName());
        } catch (SAXException e) {
            String errorMessage = "Validation failed for " + xmlFile.getName()
                    + "(" + ((SAXParseException) e).getLineNumber() + ":"
                    + ((SAXParseException) e).getColumnNumber() + "): "
                    + e.getMessage();
            errorMessages.append(errorMessage).append("\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void validateSaxXMLFile() {
        try {
            File xmlFile = new File("src/test/resources/conf/mediator/flowmanagertest-config.xml");

            // Check if file is empty
            if (!xmlFile.exists()) {
                throw new IOException("XML file does not exist: " + xmlFile.getAbsolutePath());
            }
            if (xmlFile.length() == 0) {
                throw new IOException("XML file is empty: " + xmlFile.getAbsolutePath());
            }

            System.out.format("XML file %s has been loaded successfully!", xmlFile.getCanonicalPath());

        } catch (IOException e) {
            System.err.println("IO Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}