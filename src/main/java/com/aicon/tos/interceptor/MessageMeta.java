package com.aicon.tos.interceptor;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.util.AnsiColor;
import com.avlino.common.utils.DateTimeUtils;
import com.avlino.common.utils.StringUtils;
import org.slf4j.Logger;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static com.avlino.common.Constants.LF;
import static com.avlino.common.Constants.QUESTION;

/**
 * Contains all metadata about a received message.
 */
public class MessageMeta {
    public static final String TS_OFFSET        = "CDC.Offset";  // read from the topic
    public static final String TS_CDC_RECEIVED  = "CDC.received";// when received by the consumer
    public static final String TS_DONE          = "DONE";        // when all done
    public static final String TS_START_PREFIX  = "START.";      // when scenario starts
    public static final String TS_END_PREFIX    = "END.";        // when scenario ends
    public static final String TS_READ_PREFIX   = "READ.";       // when reading data from a datasource
    public static final String TS_SEND_PREFIX   = "SEND.";       // when sending message starts
    public static final String TS_RECV_PREFIX   = "RECV.";       // when response received

    private static final String LOG_SEP         = ": ";

    private final String entityName;
    private final String messageKey;
    private long offset = -1;
    private CDCAction cdcAction;
    private Map<String, Instant> timestamps;
    private ResultEntry result = null;
    private long firstTs = 0;
    private long lastTs = 0;
    private String progress = null;
    private List<String> entityValues;

    public MessageMeta(CDCAction cdcAction, String entityName, long offset, long offsetTimestamp, String messageKey) {
        this.cdcAction = cdcAction;
        this.entityName = entityName;
        this.messageKey = messageKey;
        timestamps = new LinkedHashMap<>();
        addTimestamp(TS_OFFSET, Instant.ofEpochMilli(offsetTimestamp), null);
        addTimestamp(TS_CDC_RECEIVED, null);
        this.offset = offset;
    }

    /**
     * Sets the result only when no result stored yet or when the new result is >= in ordinal value then the existing level.
     * @param level the new level to set
     * @param text the text to add (with optional {} placeholders); when null a previous message will be preserved.
     * @param params list of optional parameters which replaces {} placeholders the same way as slf4j logging does.
     * @return the given text for logging purposes (formatted using params when given).
     */
    public String setResultWhenHigher(ResultLevel level, String text, Object... params) {
        if (StringUtils.hasContent(text) && params != null && params.length > 0) {
            text = StringUtils.format(text, params);
        }
        if (result == null || (level != null && !level.isLower(result.getLevel()))) {
            result = new ResultEntry(level, result == null ? text : (text == null ? result.getMessage() : text));
        }
        return text;
    }

    /**
     * Sets the result only when no result stored yet or when the new result is >= in ordinal value then the existing level.
     * @param logger when not null will be used to logger with level belonging to the newResult and prefixed with {@link #getAllEntityValues()}.
     * @param level the new level to set
     * @param text the text to add (with optional {} placeholders); when null a previous message will be preserved.
     * @param params list of optional parameters which replaces {} placeholders the same way as slf4j logging does.
     * @return the given text as logged
     */
    public String setResultWhenHigher(Logger logger, ResultLevel level, String text, Object... params) {
        String logText = setResultWhenHigher(level, text, params);
        log4level(logger, level, logText);
        return logText;
    }

    /**
     * Sets the result only when no result stored yet or when the new result is >= in ordinal value then the existing level.
     * @param newResult the new result to check against.
     * @return the given text for logging purposes
     */
    public String setResultWhenHigher(ResultEntry newResult) {
        if (result == null || (newResult != null && !newResult.getLevel().isLower(result.getLevel()))) {
            result = newResult;
        }
        return result.getMessage();
    }

    /**
     * Sets the result only when no result stored yet or when the new result is >= in ordinal value then the existing level.
     * @param newResult the new result to check against.
     * @return the given text for logging purposes
     */
    public String setResultWhenHigher(Logger logger, ResultEntry newResult) {
        if (result == null || (newResult != null && !newResult.getLevel().isLower(result.getLevel()))) {
            result = newResult;
        }
        if (newResult != null) {
            log4level(logger, newResult.getLevel(), newResult.getMessage());
        }
        return result.getMessage();
    }

    private String log4level(Logger logger, ResultLevel level, String logText) {
        if (level != null) {
            logText = StringUtils.concat(getAllEntityValues(), LOG_SEP, logText);
            if (logger != null) {
                switch(level) {
                    case OK     -> logger.error(AnsiColor.green     (logText));
                    case WARN   -> logger.error(AnsiColor.yellow    (logText));
                    case ERROR  -> logger.error(AnsiColor.brightRed (logText));
                }
            }
        }
        return logText;
    }

    public ResultEntry getResult() {
        return result;
    }

    public String getEntityName() {
        return entityName;
    }

    public String getActionEntityCombo() {
        return getCDCAction().getSymbol() + getEntityName();
    }

    public String getMessageKey() {
        return messageKey;
    }

    public long getOffset() {
        return offset;
    }

    public CDCAction getCDCAction() { return cdcAction; }

    public void addTimestamp(String tsKey, Logger logger) {
        addTimestamp(tsKey, Instant.now(), logger);
    }

    public void addTimestampWithPrefix(String tsPrefix, String tsAction, Logger logger) {
        addTimestamp(tsPrefix + tsAction, Instant.now(), logger);
    }

    public void addTimestamp(String tsKey, Instant ts, Logger logger) {
        lastTs = ts.toEpochMilli();
        if (firstTs == 0) {
            firstTs = lastTs;
        }
        timestamps.put(tsKey, ts);
        progress = tsKey;
        if (logger != null) {
            logger.info("MessageMeta timestamp: {} @ {}", tsKey, ts);
        }
    }

    public Instant getTimestamp(String tsKey) {
        return timestamps.get(tsKey);
    }

    /**
     * Formats timestamp with default formatter {@link DateTimeUtils#DATE_TIME_MS_FORMAT}.
     * @param tsKey the timestamp to format
     * @return the formatted timestamp
     */
    public String getTimestampAsString(String tsKey) {
        return getTimestampAsString(tsKey, DateTimeUtils.DATE_TIME_MS_FORMAT);
    }

    /**
     * Formats the requested timestamp with given DateTimeFormatter; see {@link DateTimeUtils} for a predefined list.
     * @param tsKey the ts key to format
     * @param formatter the formatter to use
     * @return the formatted timestamp
     */
    public String getTimestampAsString(String tsKey, DateTimeFormatter formatter) {
        return DateTimeUtils.formatInSystemZone(getTimestamp(tsKey), formatter);
    }

    public String getProgress() {
        return progress;
    }

    /**
     * Returns the processing time in milliseconds between first timestamp and last recorded timestamp.
     * @return processing time in ms.
     */
    public long getDurationMs() {
        return firstTs != 0 && lastTs != 0 ? lastTs - firstTs : 0;
    }

    public Map<String, Instant> getAllTimeStamps() {
        return timestamps;
    }

    public String getAllTimeStampsToString(boolean forLogging, DateTimeFormatter dtFormatter, boolean showDeltas) {
        final String OFFSET_FORMAT = "TS|offset=%10d|";
        final String TS_FORMAT = forLogging ? "|%40s = %23s" : "%s=\n%s";
        final String DELTA_FORMAT = forLogging ? "|delta = %7d ms" : " (%7dms)";
        StringBuilder sb = new StringBuilder();
        Instant firstInstant = null;
        Instant lastInstant = null;
        for (Map.Entry<String, Instant> entry: timestamps.entrySet()) {
            if (forLogging) {
                sb.append(String.format(OFFSET_FORMAT, getOffset()));
            }
            sb.append(String.format(TS_FORMAT, entry.getKey(), DateTimeUtils.formatInSystemZone(entry.getValue(), dtFormatter)));
            if (showDeltas && lastInstant != null) {
                sb.append(String.format(DELTA_FORMAT, entry.getValue().toEpochMilli() - lastInstant.toEpochMilli()));
            }
            sb.append(LF);
            lastInstant = entry.getValue();
            if (firstInstant == null) {
                firstInstant = lastInstant;
            }
        }
        if (lastInstant != null && forLogging) {
            sb.append(String.format(TS_FORMAT, getOffset(), "Total processing time", ""))
                    .append(String.format(DELTA_FORMAT, lastInstant.toEpochMilli() - firstInstant.toEpochMilli()));
        }
        return sb.toString();
    }

    public String getCdcReceivedDowTime() {
        Instant recvTS = timestamps.get(TS_CDC_RECEIVED);
        if (recvTS == null) {
            return "";
        }
        return DateTimeUtils.formatInSystemZone(recvTS, DateTimeUtils.DOW_TIME_FORMAT);
    }

    /**
     * Adds or replace a set of entity values using given format + values.
     * @param index when index not found in the list, it will add otherwise it will replace the values stored at the index.
     * @param format the formatting string using {} as placeholders.
     * @param values the list of values to replace the at the {} placeholders.
     * @return the old string when stored, else null.
     */
    public String setEntityValues(int index, String format, Object... values) {
        if (entityValues == null) {
            entityValues = new ArrayList<>(2);
        }
        String valueText = StringUtils.format(format, values);
        if (index >= 0 && index < entityValues.size()) {
            return entityValues.set(index, valueText);
        } else {
            entityValues.add(valueText);
            return null;
        }
    }

    /**
     * @return the values stored at given index, or null when not found.
     */
    public String getEntityValues(int index) {
        return entityValues != null && index >= 0 && index < entityValues.size() ?  entityValues.get(index) : null;
    }

    /**
     * Returns a concatenated string of all value strings stored.
     * @return all stored values, or null when nothing stored.
     */
    public String getAllEntityValues() {
        return entityValues != null ?  entityValues.toString() : null;
    }

    @Override
    public String toString() {
        return String.format("MsgMeta: Entity=%s, Offset=%s, TS-Offset/Received=%s/%s, Progress=%s/%s ms, MsgKey=%s, CDCAction=%s",
                entityName,
                offset,
                getTimestampAsString(TS_OFFSET),
                getTimestampAsString(TS_CDC_RECEIVED),
                getProgress(), getDurationMs(),
                messageKey,
                cdcAction
        );
    }
}
