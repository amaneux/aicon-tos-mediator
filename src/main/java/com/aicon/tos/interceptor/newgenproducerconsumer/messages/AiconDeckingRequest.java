package com.aicon.tos.interceptor.newgenproducerconsumer.messages;

import com.aicon.model.JobType;
import com.aicon.tos.interceptor.newgenproducerconsumer.GenericAvroBuilder;
import com.aicon.tos.model.PositionConverter;
import org.apache.avro.generic.GenericRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * The AiconDeckingRequest is a facility class to generate an AiconDeckingRequest.
 */
public class AiconDeckingRequest extends AiconMessage {

    private static final Logger LOG = LoggerFactory.getLogger(AiconDeckingRequest.class);

    private static final String DEFAULT_POS_VALUE = "1";

    // Field names of the aicon_decking_request topic
    public static final String FLD_REQUESTS = "requests";

    static public GenericRecord createMessage(
            String topic,
            String requestIndex,
            List<Map<String, Object>> requests) {
        Map<String, Object> input = Map.of(
                FLD_REQ_INDEX, requestIndex,
                FLD_REQ_COUNT, requests.size(),
                FLD_REQ_TIME_STAMP, System.currentTimeMillis(),
                FLD_ERROR_CODE, ERROR_CODE_OK,
                FLD_ERROR_DESC, "",
                FLD_REQUESTS, requests
        );

        return GenericAvroBuilder.buildMessage(topic, input);
    }

    static public Map<String, Object> createRequestElement(
            long wiKey,
            JobType jobType,
            String uptUserId,
            String programId,
            Map<String, Object> containerInfo,
            Map<String, String> posParts,
            String posSlot,
            boolean requestedBlockOnly
    ) {
        Map<String, Object> requestItem = new java.util.HashMap<>();

        requestItem.putAll(containerInfo);

        requestItem.put("jobType", jobType != null ? jobType.getAiconId() : JobType.UNKNOWN);
        requestItem.put("uptUserId", uptUserId);
        requestItem.put("programId", programId);
        requestItem.put("blockIndexNumber", posParts.getOrDefault(PositionConverter.POS_PART_BLOCK, DEFAULT_POS_VALUE));
        requestItem.put("bayIndexNumber", posParts.getOrDefault(PositionConverter.POS_PART_ROW, DEFAULT_POS_VALUE));
        requestItem.put("rowIndexNumber", posParts.getOrDefault(PositionConverter.POS_PART_COLUMN, DEFAULT_POS_VALUE));
        requestItem.put("tierIndexNumber", posParts.getOrDefault(PositionConverter.POS_PART_TIER, DEFAULT_POS_VALUE));
        requestItem.put("internalIndex1", String.valueOf(wiKey));
        requestItem.put("internalIndex2", posSlot);
        requestItem.put("requestedBlockOnly", requestedBlockOnly);
//        requestItem.put(FLD_ERROR_CODE, ERROR_CODE_OK);
//        requestItem.put(FLD_ERROR_DESC, "");

        return requestItem;
    }
}
