package com.aicon.tos.interceptor.newgenproducerconsumer.messages;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AiconMessage {
    private static final Logger LOG = LoggerFactory.getLogger(AiconMessage.class);

    public static final int ERROR_CODE_OK = 200;
    public static final String FLD_ERROR_CODE = "errorCode";
    public static final String FLD_ERROR_DESC = "errorDesc";

    public static final String FLD_REQ_INDEX = "requestIndex";
    public static final String FLD_REQ_COUNT = "count";
    public static final String FLD_REQ_TIME_STAMP = "timeStamp";

    static public String prettyPrintGenericRecord(GenericRecord record) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            return writer.writeValueAsString(mapper.readTree(record.toString()));
        } catch (Exception e) {
            LOG.warn("Could not prettyprint request {}, reason: e.getMessage()", record, e.getMessage());
            return String.valueOf(record);
        }
    }
}
