package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.shared.kafka.KafkaConfig;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.avro.Schema;

public class SchemaCopier {

    public static void main(String[] args) throws Exception {
        String registryUrl = "http://localhost:8081";
        String sourceSubject = "tos.TN4USER.ROAD_TRUCK_TRANSACTIONS-value";
        String targetSubject = "hvr.ROAD_TRUCK_TRANSACTIONS-value";

        registryUrl = KafkaConfig.getSchemaRegistryUrl();
        // 1. Schema Registry client
        int newSchemaId;
        try (SchemaRegistryClient client = new CachedSchemaRegistryClient(registryUrl, 10)) {

            var latest = client.getLatestSchemaMetadata(sourceSubject);
            String originalSchemaJson = latest.getSchema();

            String modifiedSchemaJson = originalSchemaJson
                    .replace("tos.TN4USER.ROAD_TRUCK_TRANSACTIONS", "hvr.inv_wi");

            Schema parsedSchema = new Schema.Parser().parse(modifiedSchemaJson);

            newSchemaId = client.register(targetSubject, parsedSchema);
        }
        System.out.printf("Schema gekopieerd naar subject '%s' met ID %d%n", targetSubject, newSchemaId);
    }
}
