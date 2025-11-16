package com.aicon.tos.shared.xml;

import com.aicon.tos.shared.exceptions.DeserializationException;
import com.aicon.tos.shared.exceptions.SerializationException;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtil {

    private XmlUtil() {
        // Prevent instantiation
    }

    public static <T> String serialize(Object requestObject, Class<T> fromClass) throws SerializationException {
        StringWriter stringWriter = new StringWriter();
        try {
            JAXBContext jaxbContext = JAXBContext.newInstance(fromClass);
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.TRUE);
            marshaller.marshal(requestObject, stringWriter);
        } catch (JAXBException e) {
            throw new SerializationException("Failed to serialize object of type: " + fromClass.getName(), e);
        }
        return stringWriter.toString();
    }


    public static <T> T deserialize(Logger logger, String response, Class<T> toClass) throws DeserializationException {
        if (logger != null) {
            logger.info("Deserialize: \n{}", prettyPrintXml(response));
        }

        try (StringReader stringReader = new StringReader(response)) {
            JAXBContext context = JAXBContext.newInstance(toClass);
            return toClass.cast(context.createUnmarshaller().unmarshal(stringReader));
        } catch (JAXBException e) {
            throw new DeserializationException("Deserialization failed for class: " + toClass.getName(), e);
        }
    }

    public static String prettyPrintXml(String rawXml) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

            Source xmlInput = new StreamSource(new StringReader(rawXml));
            StringWriter stringWriter = new StringWriter();
            Result xmlOutput = new StreamResult(stringWriter);

            transformer.transform(xmlInput, xmlOutput);
            return stringWriter.toString();

        } catch (Exception e) {
            throw new RuntimeException("Failed to pretty print XML", e);
        }
    }
}
