package com.aicon.tos.interceptor.newgenproducerconsumer;

import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import org.apache.avro.Schema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;

import static com.aicon.tos.shared.kafka.KafkaConfig.getSchemaRegistryUrl;

/**
 * Utility class responsible for loading Avro schema definitions from a schema registry.
 * This class provides methods to retrieve schemas associated with specific topics or subjects
 * by interacting with the schema registry.
 */
public class SchemaLoader {

    private static final Logger LOG = LoggerFactory.getLogger(SchemaLoader.class);

    private SchemaLoader() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Retrieves the Avro value schema associated with the given topic from the schema registry.
     * The method appends the suffix "-value" to the topic name to determine the schema subject
     * in the schema registry and fetches the corresponding schema.
     *
     * @param topic the name of the topic for which the value schema should be retrieved
     * @return the {@link Schema} object corresponding to the value schema
     *         of the specified topic, or null if the schema is not found
     */
    public static Schema getValueSchemaFromRegistry(String topic) {
        return getSchemaFromRegistry(topic + "-value");
    }

    public static Schema getKeySchemaFromRegistry(String topic) {
        return getSchemaFromRegistry(topic + "-key");
    }

    public static final SchemaRegistryClient client =
            new CachedSchemaRegistryClient(getSchemaRegistryUrl(), 10);

    private static Schema getSchemaFromRegistry(String subject) {
        try {
            String schemaString = client.getLatestSchemaMetadata(subject).getSchema();
            return schemaString != null ? new Schema.Parser().parse(schemaString) : null;
        } catch (IOException | RestClientException e) {
            throw new RuntimeException(String.format("Failed to fetch schema %s from registry, reason: %s", subject, e.getMessage()), e);
        }
    }

    public static Schema getAllFromRegistry(String subjectKeyword) {
        try {
            Collection<String> subjects = client.getAllSubjects();
            subjects.stream()
                    .filter(subject -> subject.contains(subjectKeyword)) // Filter alleen relevante
                    .forEach(filteredSubject -> LOG.info("Found Subject: {}", filteredSubject));


        } catch (IOException | RestClientException e) {
            LOG.info("Failed to fetch schema from registry", e);
        }
        return null;
    }

//    public static String toString(Schema schema) {
//        StringBuilder builder = new StringBuilder();
//        for (Schema.Field field : schema.getFields()) {
//            builder.append(field.name())
//                    .append(" (")
//                    .append(field.schema().getType()); // type (e.g., STRING, RECORD, etc.)
//            if (field.hasDefaultValue()) {
//                builder.append(", default=").append(field.defaultVal());
//            }
//            builder.append(")").append("\n");
//        }
//        return builder.toString();
//    }

}
