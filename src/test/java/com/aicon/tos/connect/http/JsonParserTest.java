package com.aicon.tos.connect.http;

import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_key;
import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_value;
import org.apache.avro.AvroTypeException;
import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecordBase;
import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Method;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class JsonParserTest {

    private static final Instant EXPECTED_TIMESTAMP = Instant.ofEpochMilli(1730279632849L);
    private static final Class<itv_job_resequence_response_value> AVRO_CLASS = itv_job_resequence_response_value.class;

    private static final String VALID_JSON_MESSAGE = """
            {
              "APIIdentifier": { "int": 12 },
              "requestIdx": "REQ-12345",
              "timestamp": 1730279632849,
              "errCode": 0,
              "errMsg": "Operation completed successfully"
            }
            """;

    private static final String INVALID_JSON_MESSAGE = """
            {
              "APIIdentifier": "InvalidType",
              "requestIdx": "REQ-12345",
              "timestamp": "notALong",
              "errCode": "InvalidType",
              "errMsg": "This is an error"
            }
            """;
    private static final Logger log = LoggerFactory.getLogger(JsonParserTest.class);

    @BeforeEach
    void setUp() {
        // Fields are pre-initialized using static constants, no runtime assignment needed.
    }

    @Test
    void testConvertJsonToAvroWithValidJson() {
        try {
            Schema schema = getClassSchema(AVRO_CLASS);
            log.info("Schema: {}", schema.getFullName());
            System.out.println("Input JSON: " + VALID_JSON_MESSAGE);

            itv_job_resequence_response_value avroRecord =
                    JsonParser.convertJsonToAvro(VALID_JSON_MESSAGE, itv_job_resequence_response_value.class);

            assertNotNull(avroRecord, "Avro record should not be null");
            assertEquals(12, avroRecord.getAPIIdentifier());
            assertTrue("REQ-12345".contentEquals(avroRecord.getRequestIdx()));
            // Compare timestamp in milliseconds
            assertEquals(EXPECTED_TIMESTAMP, avroRecord.getTimestamp());
            assertEquals(0, avroRecord.getErrCode());
            assertEquals("Operation completed successfully", avroRecord.getErrMsg().toString());
        } catch (IOException e) {
            fail("Exception should not occur with valid JSON input: " + e.getMessage());
        }
    }

    private static Schema getClassSchema(Class<? extends SpecificRecordBase> clazz) throws IOException {
        try {
            Method getClassSchema = clazz.getMethod("getClassSchema");
            return (Schema) getClassSchema.invoke(null);
        } catch (Exception e) {
            throw new IOException("Failed to retrieve Avro schema for: " + clazz.getName(), e);
        }
    }

    @Test
    void testConvertJsonToAvroWithSchemaRetrievalError() {
        class TestClassWithoutSchema extends SpecificRecordBase {
            public TestClassWithoutSchema() {
                // Prevent usage
            }

            @Override
            public void put(int i, Object v) {
                // Intentionally empty for test
            }

            @Override
            public Object get(int i) {
                return null; // Intentionally null for test
            }

            @Override
            public Schema getSchema() {
                return null; // Intentionally null for test
            }
        }

        Exception exception =
                assertThrows(IOException.class, () -> JsonParser.convertJsonToAvro(VALID_JSON_MESSAGE, TestClassWithoutSchema.class));

        assertInstanceOf(NoSuchMethodException.class, exception.getCause(),
                "Expected NoSuchMethodException due to missing getClassSchema method");
    }

    @Test
    void testConvertJsonToAvroWithInvalidJson() {
        Exception thrownException = assertThrows(
                AvroTypeException.class,
                () -> JsonParser.convertJsonToAvro(INVALID_JSON_MESSAGE, AVRO_CLASS)
        );
        assertExceptionContainsMessage(thrownException);
    }

    private void assertExceptionContainsMessage(Exception exception) {
        String expectedStartUnionMessage = "Expected start-union";
        String actualMessage = exception.getMessage();

        assertTrue(
                actualMessage.contains(expectedStartUnionMessage),
                String.format(
                        "Expected exception message to contain '%s', but got: '%s'",
                        expectedStartUnionMessage, actualMessage
                )
        );
    }

    @Test
    void testConvertAvroSpecificRecordToJson() {
        itv_job_resequence_response_value avroValueRecord = new itv_job_resequence_response_value();
        itv_job_resequence_response_key avroKeyRecord = new itv_job_resequence_response_key();
        avroValueRecord.setAPIIdentifier(null);
        avroValueRecord.setRequestIdx("REQ-12345");
        avroValueRecord.setTimestamp(Instant.ofEpochMilli(1730279632849L));
        avroValueRecord.setErrCode(0);
        avroValueRecord.setErrMsg("Operation completed successfully");

        avroKeyRecord.setApiIdentifier(avroValueRecord.getAPIIdentifier());
        avroKeyRecord.setRequestIdx(avroValueRecord.getRequestIdx());
        avroKeyRecord.setTimestamp(avroValueRecord.getTimestamp());

        try {
            String jsonValueResult = JsonParser.convertAvroToJson(avroValueRecord);
            assertNotNull(jsonValueResult, "JSON result of value record should not be null");
            JSONAssert.assertEquals("{\"APIIdentifier\":null}", jsonValueResult, false);
            JSONAssert.assertEquals("{\"requestIdx\":\"REQ-12345\"}", jsonValueResult, false); // false for strict mode off
            JSONAssert.assertEquals("{\"timestamp\": 1730279632849}", jsonValueResult, false);
            JSONAssert.assertEquals("{\"errCode\": 0}", jsonValueResult, false);
            JSONAssert.assertEquals("{\"errMsg\":  \"Operation completed successfully\"}", jsonValueResult, false);

            String jsonKeyResult = JsonParser.convertAvroToJson(avroValueRecord);
            assertNotNull(jsonKeyResult, "JSON result of key record should not be null");
            JSONAssert.assertEquals("{\"APIIdentifier\":null}", jsonKeyResult, false);
            JSONAssert.assertEquals("{\"requestIdx\":\"REQ-12345\"}", jsonKeyResult, false); // false for strict mode off
            JSONAssert.assertEquals("{\"timestamp\": 1730279632849}", jsonKeyResult, false);

        } catch (JSONException e) {
            fail("Exception should not occur during Avro to JSON conversion: " + e.getMessage());
        }
    }
}
