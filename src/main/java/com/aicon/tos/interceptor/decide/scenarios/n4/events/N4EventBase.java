package com.aicon.tos.interceptor.decide.scenarios.n4.events;

import com.aicon.tos.interceptor.CollectedMessage;
import com.aicon.tos.interceptor.FilteredMessage;
import com.aicon.tos.interceptor.MessageMeta;

import java.time.Instant;

/**
 * Base class for all N4 entity events. Acts also as the factory to provide the right Entity Event class for easy access
 * of commonly used fields.
 */
abstract public class N4EventBase {

    protected FilteredMessage msg;

    public static <T extends N4EventBase> T createInstance(FilteredMessage msg) {
        try {
            if (WorkInstructionEvent.ENTITY_NAME.equalsIgnoreCase(msg.getEntityName())) {
                return (T) WorkInstructionEvent.class.getDeclaredConstructor(FilteredMessage.class).newInstance(msg);
            } else if (RoadTruckTransactionEvent.ENTITY_NAME.equalsIgnoreCase(msg.getEntityName())) {
                return (T) RoadTruckTransactionEvent.class.getDeclaredConstructor(FilteredMessage.class).newInstance(msg);
            } else {
                throw new IllegalArgumentException("Unknown event for entity: " + msg.getEntityName());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not create instance for entity: " + msg.getEntityName(), e);
        }
    }

    public N4EventBase(FilteredMessage msg) {
        this.msg = msg;
    }

    public FilteredMessage getMsg() {
        return msg;
    }

    public String getEntityName() {
        return msg.getEntityName();
    }

    public Instant getCdCReceivedTimestamp() {
        return msg.meta().getTimestamp(MessageMeta.TS_CDC_RECEIVED);
    }

    public String getMessageKey() {
        return msg.getMessageKey();
    }
}
