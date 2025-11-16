package com.aicon.tos.connect.http;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.avro.Schema;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.apache.avro.io.EncoderFactory;
import org.apache.avro.io.JsonEncoder;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class JsonParser {

    private static final Logger LOG = LoggerFactory.getLogger(JsonParser.class);

    private JsonParser() { /* Prevent usage */ }

    private static <T extends SpecificRecordBase> Schema getSchema(Class<T> avroClass) throws IOException {
        try {
            return (Schema) avroClass.getMethod("getClassSchema").invoke(null);
        }
        catch (IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
            throw new IOException("Failed to retrieve schema for Avro class: " + avroClass.getName(), e);
        }
    }

    public static <T extends SpecificRecordBase> T convertJsonToAvro(String json, Class<T> avroClass)
        throws IOException {
        Schema schema = getSchema(avroClass);
        DatumReader<T> reader = new SpecificDatumReader<>(schema);
        Decoder decoder = DecoderFactory.get().jsonDecoder(schema, json);
        return reader.read(null, decoder);
    }

    public static String convertAvroToJson(SpecificRecordBase avroObject) {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            DatumWriter<SpecificRecordBase> writer = new SpecificDatumWriter<>(avroObject.getSchema());
            JsonEncoder jsonEncoder = EncoderFactory.get().jsonEncoder(avroObject.getSchema(), outputStream);

            writer.write(avroObject, jsonEncoder);
            jsonEncoder.flush();

            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(outputStream.toString("UTF-8"));
            JsonNode cleanedJsonNode = cleanUnionTypes(jsonNode);

            return objectMapper.writeValueAsString(cleanedJsonNode);
        }
        catch (IOException e) {
            // Return a meaningful fallback (e.g., an empty JSON or null) or handle it properly
            LOG.error("Error converting Avro to JSON: {}", e.getMessage(), e); // Log the error
            return "{}"; // Returning empty JSON as a fallback value
        }
    }

    private static JsonNode cleanUnionTypes(JsonNode node) {
        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode childNode = entry.getValue();

                // Check for union encoding pattern (object with one field)
                if (childNode.isObject() && childNode.size() == 1) {
                    Map.Entry<String, JsonNode> singleField = childNode.fields().next();
                    if ("null".equals(singleField.getKey())) {
                        // Set to null if it's the "null" type
                        objectNode.set(entry.getKey(), null);
                    }
                    else {
                        // Replace with the actual value if not null
                        objectNode.set(entry.getKey(), singleField.getValue());
                    }
                }
                else {
                    // Recursively clean nested objects
                    cleanUnionTypes(childNode);
                }
            }
        }
        else if (node.isArray()) {
            for (JsonNode item : node) {
                cleanUnionTypes(item);
            }
        }
        return node;
    }
}
