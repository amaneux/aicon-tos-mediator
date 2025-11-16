package com.aicon.tos.shared.kafka;

import generated.AiconYardDeckingUpdateRequest;

public class AiconYardDeckingUpdateRequestHelper {

    private AiconYardDeckingUpdateRequestHelper() {
        throw new IllegalStateException("Utility class");
    }

    public static AiconYardDeckingUpdateRequest createWithDefaults() {
        AiconYardDeckingUpdateRequest request = new AiconYardDeckingUpdateRequest();
        if (request.getMoves() == null) {
            request.setMoves(new AiconYardDeckingUpdateRequest.Moves());
        }
        return request;
    }
}