package com.aicon.tos.interceptor.decide.scenarios.n4.messages;

import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.avlino.common.Constants;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Helper class representing the TOS Decking Update Response schema (parsing the aicon-yard-decking-update-response).
 */
public class TosDeckingUpdateResponse {

    enum TosSeverity {
        OK      (ResultLevel.OK),
        INFO    (ResultLevel.OK),
        WARN    (ResultLevel.WARN),
        ERROR   (ResultLevel.ERROR),
        FATAL   (ResultLevel.ERROR);

        ResultLevel level;

        TosSeverity(ResultLevel level) {
            this.level = level;
        }

        ResultLevel getResultLevel() {
            return level;
        }
    }

    private ResultEntry worseResult;
    private String requestId = null;
    private long timeStamp = 0;

    /**
     * @return the worst reported result.
     */
    public ResultEntry getWorseResult() {
        return worseResult;
    }

    /**
     * Factory method to convert a GenericRecord into an AiconDeckingEngineResponse.
     * We're now only focused on getting the worst report result.
     */
    public static TosDeckingUpdateResponse fromGenericRecord(GenericRecord genericRecord) {
        TosDeckingUpdateResponse response = new TosDeckingUpdateResponse();

        try {
            // Map top-level fields
            response.requestId = getStringValue(genericRecord.get("requestId"));
            response.timeStamp = ((Long) genericRecord.get("timeStamp"));
            response.worseResult = getResult((GenericRecord) genericRecord.get("result"));

            // Map nested "moves" list, but for now we're only interested in the results
            List<GenericRecord> moves = (List<GenericRecord>) genericRecord.get("moves");
            if (moves != null) {
                for (GenericRecord move : moves) {
                    ResultEntry moveResult = getResult((GenericRecord) move.get("result"));
                    if (!moveResult.getLevel().isLower(response.worseResult.getLevel())) {
                        response.worseResult = moveResult;
                    }
                }
            }
        } catch (Exception e) {
            response.worseResult = new ResultEntry(ResultLevel.ERROR, e.getMessage());
        }

        return response;
    }

    private static ResultEntry getResult(GenericRecord rec) {
        String level = getStringValue(rec.get("resultLevel"), TosSeverity.OK.name());
        String text = getStringValue(rec.get("resultText"), null);
        ResultEntry resultEntry = new ResultEntry(TosSeverity.valueOf(level).getResultLevel(), text);
        resultEntry.setCode(getStringValue(rec.get("resultCode"), null));
        return resultEntry;
    }

    private static String getStringValue(Object value) {
        return getStringValue(value, Constants.EMPTY);
    }

    private static String getStringValue(Object value, String nullValue) {
        if (value == null) {
            return nullValue;
        }
        return value.toString();
    }
}