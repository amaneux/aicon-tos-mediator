package com.avlino.common.utils;

import com.avlino.common.Constants;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.logging.Logger;

/**
 * Convenience class for data time conversions & calculations
 */
public class DateTimeUtils {
    private static final Logger LOG = Logger.getLogger(DateTimeUtils.class.getName());

    public static final SimpleDateFormat SD_DATE_TIME_FORMAT    = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final DateTimeFormatter DATE_TIME_FORMAT      = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    public static final DateTimeFormatter DATE_TIME_MS_FORMAT   = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    public static final DateTimeFormatter DATE_TIME_ZONE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss XXX");
    public static final DateTimeFormatter DOW_TIME_FORMAT       = DateTimeFormatter.ofPattern("EEE HH:mm:ss");    //DOW = Day of Week
    public static final DateTimeFormatter DOW_TIME_MS_FORMAT    = DateTimeFormatter.ofPattern("EEE HH:mm:ss.SSS");
    public static final DateTimeFormatter TIME_FORMAT           = DateTimeFormatter.ofPattern("HH:mm:ss");
    public static final DateTimeFormatter TIME_MS_FORMAT        = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    public static final DateTimeFormatter TIME_ZONE_FORMAT      = DateTimeFormatter.ofPattern("HH:mm:ss XXX");
    public static final DateTimeFormatter ISO8601_FORMAT        = DateTimeFormatter.ISO_DATE_TIME;
    public static final DateTimeFormatter FILE_TS_FORMAT        = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public static Instant now() {
        return Instant.now();
    }

    public static Date nowDate() {
        return new Date();
    }

    public static long nowMillis() {
        return System.currentTimeMillis();
    }

    public static String formatNowInSystemZone(DateTimeFormatter formatter) {
        return formatInSystemZone(now(), formatter);
    }

    public static String formatInSystemZone(Date ts, DateTimeFormatter formatter) {
        return formatInSystemZone(ts.toInstant(), formatter);
    }

    public static String formatInSystemZone(long ts, DateTimeFormatter formatter) {
        return formatInSystemZone(Instant.ofEpochMilli(ts), formatter);
    }

    public static String formatInSystemZone(Instant ts, DateTimeFormatter formatter) {
        return ts.atZone(ZoneId.systemDefault()).format(formatter);
    }

    public static ZonedDateTime getZonedNow(String timeZone) {
        return ZonedDateTime.now(ensureZoneId(timeZone));
    }

    public static ZonedDateTime getZonedDateTime(Instant ts, String timeZone) {
        return ts.atZone(ensureZoneId(timeZone));
    }

    public static ZonedDateTime getZonedDateTime(Date ts, String timeZone) {
        return ts.toInstant().atZone(ensureZoneId(timeZone));
    }

    public static ZonedDateTime getZonedDateTime(long ts, String timeZone) {
        return Instant.ofEpochMilli(ts).atZone(ensureZoneId(timeZone));
    }

    public static String formatNowZoned(String timeZone, DateTimeFormatter formatter) {
        return getZonedNow(timeZone).format(formatter);
    }

    public static String formatZonedDateTime(Instant ts, String timeZone, DateTimeFormatter formatter) {
        return getZonedDateTime(ts, timeZone).format(formatter);
    }

    public static String formatZonedDateTime(Date ts, String timeZone, DateTimeFormatter formatter) {
        return getZonedDateTime(ts, timeZone).format(formatter);
    }

    public static String formatZonedDateTime(long ts, String timeZone, DateTimeFormatter formatter) {
        return getZonedDateTime(ts, timeZone).format(formatter);
    }

    public static Instant parseZonedDateTime(String timeStamp, ZoneId timeZone, DateTimeFormatter formatter) {
        LocalDateTime dateTime = LocalDateTime.parse(timeStamp, formatter);
        return dateTime.atZone(timeZone).toInstant();
    }

    /**
     * @param timeZone the timeZone as a string
     * @return the ZoneId when matching the timeZone or else system default time zone
     */
    public static ZoneId ensureZoneId(String timeZone) {
        ZoneId zone = null;
        if (StringUtils.hasContent(timeZone)) {
            try {
                zone = ZoneId.of(timeZone);
            } catch (Exception e) {
                LOG.warning(String.format("Parsing timezone %s failed (switch to system default), reason: %s ", timeZone, e.getMessage()));
            }
        }
        if (zone == null) {
            zone = ZoneId.systemDefault();
        }

        return zone;
    }

    /**
     * Returns a named time difference based on the most appropiate time unit based on the length of the period, like:
     * 4 mins ago, 30 hours later, 2 days ago etc
     * @param tsMillis the timestamp to calculate against now()
     * @param shortName when true uses mins instead of minutes
     * @return a named time difference based on the best fitting time unit.
     */
    public static String getNamedTimeDifference(long tsMillis, boolean shortName) {
        if (tsMillis == 0) {
            return Constants.UNKNOWN;
        }

        // For server-side calculation
        long now = System.currentTimeMillis();
        long diff = (tsMillis - now) / 1000;

        String agoLater = diff < 0 ? "ago" : "later";
        diff = Math.abs(diff);
        if (diff < 60)      return "just now";
        if (diff < 3600)    return String.format("%s %s %s", (diff / 60  ), selectString(shortName, "mins", "minutes"), agoLater);
        if (diff < 2*86400) return String.format("%s %s %s", (diff / 3600), selectString(shortName, "hrs" , "hours"  ), agoLater);
        return (diff / 86400) + " days ago";
    }

    private static String selectString(boolean selected, String... str) {
        return str != null && str.length >= 2 ? (selected ? str[0] : str[1]) : Constants.UNKNOWN;
    }
}
