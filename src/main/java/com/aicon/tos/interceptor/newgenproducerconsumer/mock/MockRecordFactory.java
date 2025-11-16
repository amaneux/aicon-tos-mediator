package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.interceptor.newgenproducerconsumer.JsonToAvroMessageBuilder;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;

public class MockRecordFactory {

    public static List<GenericRecord> generateRecords(Schema schema, int count) {
        List<GenericRecord> list = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String json = String.format("""
                {
                  "before": {
                    "hvr.inv_wi.Value": {
                      "gkey": %d,
                      "door_direction": "NORTH",
                      "locked": false,
                      "confirmed": true,
                      "che_id": %d
                    }
                  },
                  "after": {
                    "hvr.inv_wi.Value": {
                      "gkey": %d,
                      "door_direction": "SOUTH",
                      "locked": true,
                      "confirmed": false,
                      "che_id": %d
                    }
                  },
                  "source": {
                    "version": "1.0",
                    "connector": "mock",
                    "name": "mock",
                    "ts_ms": 0,
                    "snapshot": { "string": "false" },
                    "db": "mock",
                    "schema": "mock",
                    "table": "mock"
                  },
                  "op": "u",
                  "ts_ms": null,
                  "transaction": null
                }
                """, 1000 + i, 2000 + i, 3000 + i, 4000 + i);

            GenericRecord record = JsonToAvroMessageBuilder.createMessageFromJson( schema, json);
            list.add(record);
        }
        return list;
    }
}

