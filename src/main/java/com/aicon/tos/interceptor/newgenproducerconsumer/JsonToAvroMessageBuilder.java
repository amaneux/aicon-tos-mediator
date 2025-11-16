package com.aicon.tos.interceptor.newgenproducerconsumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.io.Decoder;
import org.apache.avro.io.DecoderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JsonToAvroMessageBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(JsonToAvroMessageBuilder.class);

    private JsonToAvroMessageBuilder() {
        throw new IllegalStateException("Utility class");
    }

    public static GenericRecord createMessageFromJson(Schema schema, String json) {
        try {
            LOG.info("========== JsonToAvroMessageBuilder ==========");
            LOG.info("Input JSON:\n{}", json);
            LOG.info("Using schema:\n{}", schema);

            ObjectMapper objectMapper = new ObjectMapper();
            ObjectNode inputNode = (ObjectNode) objectMapper.readTree(json);
            ObjectNode enrichedNode = JsonEnricher.enrichJson(schema, inputNode);
            String enriched = enrichedNode.toString();

            LOG.info("Enriched JSON:\n{}", enriched);

            Decoder decoder = DecoderFactory.get().jsonDecoder(schema, enriched);
            GenericDatumReader<GenericRecord> reader = new GenericDatumReader<>(schema);
            return reader.read(null, decoder);
        } catch (IOException e) {
            LOG.error("IO exception during Avro conversion", e);
        } catch (Exception e) {
            LOG.error("Generic exception during Avro conversion", e);
        }
        return null;
    }
}
