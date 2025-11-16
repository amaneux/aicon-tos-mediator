package com.aicon.tos.shared.kafka;

import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;

import java.util.ArrayList;

public class AiconYardDeckingUpdateRequestMessageHelper {

    private AiconYardDeckingUpdateRequestMessageHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static AiconYardDeckingUpdateRequestMessage createWithDefaults() {
        AiconYardDeckingUpdateRequestMessage request = new AiconYardDeckingUpdateRequestMessage();
        if (request.getMoves() == null) {
            request.setMoves(new ArrayList<>());
        }
        return request;
    }
}