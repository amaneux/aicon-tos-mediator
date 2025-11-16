package com.aicon.tos.connect.http.transformers;

import com.aicon.tos.shared.exceptions.SoapResponseTransformationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * A class that implements the transformation of SOAP requests and responses.
 * <p>
 * This transformer wraps a plain request into a SOAP envelope to make it valid for
 * SOAP communication and extracts the relevant content of the response from the SOAP body.
 * </p>
 */
public class SoapTransformer implements RequestResponseTransformer {

    /**
     * Transforms a plain request by wrapping it with a SOAP envelope.
     *
     * @param request the plain request content to be wrapped in a SOAP envelope.
     * @return the complete SOAP envelope containing the original request as a String.
     */
    @Override
    public String transformRequest(String request, String n4Scope) {
        // Build the SOAP envelope prefix
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" ");
        stringBuilder.append("xmlns:arg=\"http://www.navis.com/services/argobasicservice\">\n");
        stringBuilder.append("<soapenv:Header/>\n");
        stringBuilder.append("<soapenv:Body>\n");
        stringBuilder.append("<arg:basicInvoke>\n");
        stringBuilder.append("<arg:scopeCoordinateIds>").append(n4Scope).append("</arg:scopeCoordinateIds>\n");
        stringBuilder.append("<arg:xmlDoc><![CDATA[\n");
        stringBuilder.append("<custom class=\"AlenzaYardsightWebserviceListener\" type=\"extension\">");

        String soapPrefix = stringBuilder.toString();

        // Reset the StringBuilder to generate the SOAP envelope suffix
        stringBuilder.delete(0, stringBuilder.length());
        stringBuilder.append("</custom>\n");
        stringBuilder.append("    ]]></arg:xmlDoc>\n");
        stringBuilder.append("</arg:basicInvoke>\n");
        stringBuilder.append("</soapenv:Body>\n");
        stringBuilder.append("</soapenv:Envelope>");
        String soapPostfix = stringBuilder.toString();

        // Return the combined SOAP envelope with the request content
        return soapPrefix + request + soapPostfix;
    }

    /**
     * Extracts the content of the SOAP body from the given SOAP response.
     *
     * @param soapResponse the SOAP response as an XML string.
     * @return the content within the `<soap:Body>` tag as a String.
     * @throws SoapResponseTransformationException if the response is invalid,
     *                                             parsing fails, or the `<soap:Body>` is missing.
     */
    @Override
    public String transformResponse(String soapResponse) {
        try {
            // Step 1: Parse the SOAP response
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true); // Required to handle namespaces
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new InputSource(new StringReader(soapResponse)));

            // Step 2: Find the Body element in the SOAP message
            NodeList bodyNodes = document.getElementsByTagNameNS("*", "Body");
            if (bodyNodes.getLength() == 0) {
                throw new SoapResponseTransformationException("SOAP Body not found in response.");
            }

            Node bodyNode = bodyNodes.item(0);

            // Step 3: Locate the <basicInvokeResponse> node
            NodeList basicInvokeNodes = ((Element) bodyNode).getElementsByTagNameNS("*", "basicInvokeResponse");
            if (basicInvokeNodes.getLength() == 0) {
                throw new SoapResponseTransformationException("<basicInvokeResponse> not found in SOAP Body.");
            }

            Node basicInvokeNode = basicInvokeNodes.item(0);

            // Step 4: Get the text content of <basicInvokeResponse> (escaped XML)
            String escapedXml = basicInvokeNode.getTextContent();
            if (escapedXml == null || escapedXml.isEmpty()) {
                throw new SoapResponseTransformationException("No content found inside <basicInvokeResponse>.");
            }

            // Step 5: Unescape the XML string
            String unescapedXml = org.apache.commons.text.StringEscapeUtils.unescapeXml(escapedXml);

            // Step 6: Parse the unescaped XML string separately
            Document unescapedDocument = builder.parse(new InputSource(new StringReader(unescapedXml)));

            // Step 7: Locate the <argo:custom-response> node
            NodeList customResponseNodes = unescapedDocument.getElementsByTagNameNS("http://www.navis.com/argo", "custom-response");
            if (customResponseNodes.getLength() == 0) {
                throw new SoapResponseTransformationException("<argo:custom-response> not found in the unescaped response.");
            }

            Node customResponseNode = customResponseNodes.item(0);

            // Step 8: Extract the inner content of <argo:custom-response> as a string
            StringBuilder resultBuilder = new StringBuilder();
            Node childNode = customResponseNode.getFirstChild();
            while (childNode != null) {
                resultBuilder.append(nodeToString(childNode));
                childNode = childNode.getNextSibling();
            }

            return resultBuilder.toString().trim();

        } catch (Exception e) {
            throw new SoapResponseTransformationException("Error while transforming SOAP response", e);
        }
    }

    /**
     * Helper method to convert a Node into a String.
     */
    private String nodeToString(Node node) {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "no");

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(node), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error converting XML Node to String", e);
        }
    }
}