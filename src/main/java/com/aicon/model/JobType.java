package com.aicon.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonDeserialize(using = JobType.Deserializer.class)
public enum JobType {
    GATE_IN     ("GI"),
    RAIL_IN     ("DS"),
    VESSEL_IN   ("DS"),
    YARD_MOVE   ("RH"),
    YARD_SHIFT  ("AR"),
    UNKNOWN     (null); // Special enum value for `null` or unknown cases

    private final String aiconId;

    JobType(String aiconId) {
        this.aiconId = aiconId;
    }

    public String getAiconId() {return aiconId;}

    /**
     * Retrieve an enum instance by the aiconId.
     * @param aiconId The AiconId to search for.
     * @return The corresponding `WiMoveKindEnum`, or `UNKNOWN` if the code is null or invalid.
     */
    public static JobType findForAiconId(String aiconId) {
        for (JobType jobType : JobType.values()) {
            if (jobType.getAiconId().equals(aiconId)) {
                return jobType;
            }
        }
        return UNKNOWN;
    }

    /**
     * Custom deserializer for `JobType`.
     */
    public static class Deserializer extends StdDeserializer<JobType> {

        public Deserializer() {
            super(JobType.class);
        }

        @Override
        public JobType deserialize(JsonParser parser, DeserializationContext ctxt) throws IOException {
            String value = parser.getText(); // Get the string value from JSON
            return JobType.findForAiconId(value); // Convert to the enum based on code
        }
    }
}