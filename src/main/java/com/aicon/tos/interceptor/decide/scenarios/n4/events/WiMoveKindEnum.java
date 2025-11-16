package com.aicon.tos.interceptor.decide.scenarios.n4.events;

import com.aicon.model.JobType;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(using = WiMoveKindEnum.Deserializer.class)
public enum WiMoveKindEnum {
    RECEIVAL        ("RECV", JobType.GATE_IN),
    DELIVERY        ("DLVR", null),
    VESL_DISCH      ("DSCH", JobType.VESSEL_IN),
    VESL_LOAD       ("LOAD", null),
    RAIL_DISCH      ("RDSC", JobType.RAIL_IN),
    RAIL_LOAD       ("RLOD", null),
    SHIFT_ON_BOARD  ("SHOB", null),
    YARD_MOVE       ("YARD", JobType.YARD_MOVE),
    YARD_SHIFT      ("SHFT", JobType.YARD_SHIFT),
    OTHER           ("OTHR", null),
    UNKNOWN         (null  , null); // Special enum value for `null` or unknown cases

    private final String tosCode;
    private final JobType jobType;

    // Internal static map for quick lookup by code
    private static final Map<String, WiMoveKindEnum> ENUM_MAP;

    static {
        ENUM_MAP = new HashMap<>();
        for (WiMoveKindEnum kind : WiMoveKindEnum.values()) {
            ENUM_MAP.put(kind.tosCode, kind); // Map code to enum (e.g., "RECV" -> RECEIVAL)
        }
    }

    WiMoveKindEnum(String code, JobType jobType) {
        this.tosCode = code;
        this.jobType = jobType;
    }

    public String getTosCode() {
        return tosCode;
    }

    public JobType getJobType() {return jobType;}

    /**
     * Retrieve an enum instance by its code.
     *
     * @param code The code to search for.
     * @return The corresponding `WiMoveKindEnum`, or `UNKNOWN` if the code is null or invalid.
     */
    public static WiMoveKindEnum getEnumForCode(String code) {
        return ENUM_MAP.getOrDefault(code, UNKNOWN);
    }

    /**
     * Custom deserializer for `WiMoveKindEnum`.
     */
    public static class Deserializer extends StdDeserializer<WiMoveKindEnum> {

        public Deserializer() {
            super(WiMoveKindEnum.class);
        }

        @Override
        public WiMoveKindEnum deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            String value = parser.getText(); // Get the string value from JSON
            return WiMoveKindEnum.getEnumForCode(value); // Convert to the enum based on code
        }
    }
}