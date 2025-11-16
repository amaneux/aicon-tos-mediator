package com.aicon.tos.interceptor.decide.scenarios.n4.events;

import com.aicon.tos.interceptor.FilteredMessage;
import com.avlino.common.MetaField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WorkInstructionEvent extends N4EventBase {

    private static final Logger LOG = LoggerFactory.getLogger(WorkInstructionEvent.class);

    public enum EcStateFetch {
        NONE,
        AUTO_FETCH,
        IMMINENT_MOVE,
        PRIORITY_FETCH,
        PRIORITY_DISPATCH,
        STANDARD_FETCH
    }

    public enum MoveStage {
        NONE,
        PLANNED,
        FETCH_UNDERWAY,
        CARRY_READY,
        CARRY_UNDERWAY,
        CARRY_COMPLETE,
        PUT_UNDERWAY,
        PUT_COMPLETE,
        COMPLETE;
    }

    public enum TwinWith {
        NONE,
        NEXT,
        PREV
    }

    public static final String ENTITY_NAME          = "inv_wi";

    public static final String FLD_DEFINITE         = "definite";
    public static final String FLD_EC_STATE_FETCH   = "ec_state_fetch";
    public static final String FLD_GKEY             = "gkey";
    public static final String FLD_ITV_GKEY         = "itv_gkey";
    public static final String FLD_MOVE_KIND        = "move_kind";
    public static final String FLD_MOVE_STAGE       = "move_stage";
    public static final String FLD_MOVE_NUMBER      = "move_number";
    public static final String FLD_POS_SLOT         = "pos_slot";
    public static final String FLD_POS_NAME         = "pos_name";
    public static final String FLD_SEQUENCE         = "sequence";
    public static final String FLD_TRAN_GKEY        = "tran_gkey";
    public static final String FLD_TWIN_WITH        = "twin_with";
    public static final String FLD_WQ_GKEY          = "work_queue_gkey";

    public static final MetaField<?> MF_ITV_GKEY = new MetaField<>(FLD_ITV_GKEY, Long.class);
    public static final MetaField<?> MF_POS_NAME = new MetaField<>(FLD_POS_SLOT, String.class);
    public static final MetaField<?> MF_EC_STATE_FETCH = new MetaField<>(FLD_EC_STATE_FETCH, String.class);
    public static final MetaField<?> MF_MOVE_STAGE = new MetaField<>(FLD_MOVE_STAGE, String.class);

    public WorkInstructionEvent(FilteredMessage msg) {
        super(msg);
    }

    //------- Dedicated getters for specific fields -------//
    public Long getGkey() {
        return msg.getFieldValueAsLong(FLD_GKEY, -1L);
    }

    public Long getItvGkey() {
        return msg.getFieldValueAsLong(FLD_ITV_GKEY, -1L);
    }

    public String getPosName() {
        return msg.getFieldValueAsString(FLD_POS_NAME);
    }

    public String getPosSlot() {
        return msg.getFieldValueAsString(FLD_POS_SLOT);
    }

    public Long getSequence() {
        return msg.getFieldValueAsLong(FLD_SEQUENCE, -1L);
    }

    public Long getTranGkey() {
        return msg.getFieldValueAsLong(FLD_TRAN_GKEY, -1L);
    }

    public Long getWqGkey() {
        return msg.getFieldValueAsLong(FLD_WQ_GKEY, -1L);
    }

    public WiMoveKindEnum getMoveKind() {
        String val = msg.getFieldValueAsString(FLD_MOVE_KIND);
        return WiMoveKindEnum.getEnumForCode(val);
    }

    public Double getMoveNumber() {
        return msg.getFieldValueAsDouble(FLD_MOVE_NUMBER, 0.0);
    }

    public MoveStage getMoveStage() {
        String val = msg.getFieldValueAsString(FLD_MOVE_STAGE);
        try {
            return MoveStage.valueOf(val);
        } catch (IllegalArgumentException e) {
            LOG.error("Unknown move_stage={} for wi.gkey={}", val, getGkey());
            return null;
        }
    }

    /**
     * Returns the value of field {@link #FLD_EC_STATE_FETCH} in safe way (taking care of null and errors)
     * @param nullValue will be returned when the underlying value is null or not valid for the enum.
     * @return the enum when valid or else the <code>nullValue</code>.
     */
    public EcStateFetch getEcStateFetch(EcStateFetch nullValue) {
        String esf = msg.getFieldValueAsString(FLD_EC_STATE_FETCH);
        if (esf == null) {
            return nullValue;
        } else {
            try {
                return EcStateFetch.valueOf(esf);
            } catch (Exception e) {
                return nullValue;
            }
        }
    }

    /**
     * Returns the value of field {@link #FLD_TWIN_WITH} in safe way (taking care of null and errors)
     * @param nullValue will be returned when the underlying value is null or not valid for the enum.
     * @return the enum when valid or else the <code>nullValue</code>.
     */
    public TwinWith getTwinWith(TwinWith nullValue) {
        return getTwinWith(msg.getFieldValueAsString(FLD_TWIN_WITH), nullValue);
    }

    /**
     * Returns the value of <code>twValue</code> in safe way (taking care of null and errors).
     * @param twValue the value of twin_with
     * @param nullValue will be returned when the underlying value is null or not valid for the enum.
     * @return the enum when valid or else the <code>nullValue</code>.
     */
    public TwinWith getTwinWith(String twValue, TwinWith nullValue) {
        if (twValue == null) {
            return nullValue;
        } else {
            try {
                return TwinWith.valueOf(twValue);
            } catch (Exception e) {
                LOG.error("Unknown twin_with={} for wi.gkey={}", twValue, getGkey());
                return nullValue;
            }
        }
    }
}
