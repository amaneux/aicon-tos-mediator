package com.aicon.tos.interceptor.decide.scenarios.n4.events;

import com.aicon.tos.interceptor.FilteredMessage;

public class RoadTruckTransactionEvent extends N4EventBase {

    public static final String ENTITY_NAME = "road_truck_transactions";

    public static final String FLD_GKEY = "gkey";
    public static final String FLD_CTR_POS_NAME = "ctr_pos_name";
    public static final String FLD_EC_STATE_FETCH = "ec_state_fetch";

    public RoadTruckTransactionEvent(FilteredMessage msg) {
        super(msg);
    }

    public String getCtrPosName() {
        return msg.getFieldValueAsString(FLD_CTR_POS_NAME);
    }

    public Long getGkey() {
        return msg.getFieldValueAsLong(FLD_GKEY, -1L);
    }

    public String getEcStateFetch() {return msg.getFieldValueAsString(FLD_EC_STATE_FETCH); }
}
