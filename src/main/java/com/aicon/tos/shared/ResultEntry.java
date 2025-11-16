package com.aicon.tos.shared;

import com.avlino.common.Constants;

/**
 * Adds information about the result of any kind of operation. Next to the level, a message explaining the problem
 * (in the form of a free text or an exception) is typically set as well. A code can also be set in a later phase
 * which is some agreed upon list of codes for various parties (but not enforced here).
 */
public class ResultEntry {

    public enum MessageAction {
        ADD_BEFORE,
        ADD_AFTER,
        REPLACE
    }

    private ResultLevel level   = null;
    private String code         = null;     // any agreed upon code between 2 parties
    private String message      = null;     // allows any free text
    private Throwable exception = null;     // keeps the exception for later reference when more details are needed

    /**
     * Creates a Result Entry with level and a free text message.
     * @param level the level to set
     * @param msg the free text
     */
    public ResultEntry(ResultLevel level, String msg) {
        this.level = level;
        this.message = msg;
    }

    /**
     * Creates a Result Entry with level and a message derived from the given ex.getMessage().
     * @param level the level to set
     * @param ex the Exception occurred, getMessage() will be used to populate the internal message field.
     */
    public ResultEntry(ResultLevel level, Throwable ex) {
        this.level = level;
        if (ex != null) {
            this.exception = ex;
            this.message = ex.getMessage();
        }
    }

    /**
     * Creates a Result Entry with level only (leaving message and exception null).
     * @param level the level to set
     */
    public ResultEntry(ResultLevel level) {
        this(level, (String)null);
    }

    public ResultEntry setCode(String code) {
        this.code = code;
        return this;
    }

    public ResultEntry setCode(Integer code) {
        this.code = String.valueOf(code);
        return this;
    }

    public ResultEntry setException(Throwable ex) {
        this.exception = ex;
        return this;
    }

    /**
     * Overrules the current level when new level is equal or worse. In this case message can be updated as well.
     * @param level the level, should be at >= current level
     * @param message the message to override when level >= current level
     * @return this object
     */
    public ResultEntry overrideWhenNOK(ResultLevel level, String message) {
        if (!level.isLower(getLevel())) {
            this.level = level;
            if (message != null && this.message == null) {
                this.message = message;
            }
        }
        return this;
    }

    public String getCode() {
        return code;
    }

    public ResultEntry setMessage(String text, MessageAction action, String delimiter) {
        if (action == null) {
            action = MessageAction.REPLACE;
        }
        delimiter = delimiter == null ? Constants.EMPTY : delimiter;
        switch (action) {
            case ADD_BEFORE -> message = message != null ? text + delimiter + message : text;
            case ADD_AFTER  -> message = message != null ? message + delimiter + text : text;
            case REPLACE    -> message = text;
        }
        return this;
    }

    public String getMessage() {
        return message;
    }

    public ResultLevel getLevel() {
        return level != null ? level : ResultLevel.OK;
    }

    public Throwable getException() {
        return exception;
    }

    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append(level);
        if (code != null) {
            sb.append(":").append(code);
        }
        if (message != null) {
            sb.append(" (").append(message).append(")");
        }
        return sb.toString();
    }
}
