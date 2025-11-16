package com.aicon.tos.shared.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Duration;
import java.time.Instant;
import java.util.GregorianCalendar;

/**
 * Utility class for time-related operations.
 */
public class TimeUtils {

    private static final Logger LOG = LoggerFactory.getLogger(TimeUtils.class);

    private TimeUtils() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Formats milliseconds as a human-readable time string.
     *
     * @param ms duration in milliseconds
     * @return formatted string in HH:mm:ss.SSS format
     */
    public static String formatMilliseconds(long ms) {
        long hours = ms / 3_600_000;
        long minutes = (ms % 3_600_000) / 60_000;
        long seconds = (ms % 60_000) / 1_000;
        long milliseconds = ms % 1_000;

        return String.format("%02d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds);
    }

    public static XMLGregorianCalendar convertToXMLGregorianCalendar(Instant instant) {
        // Convert Instant to GregorianCalendar
        GregorianCalendar calendar = new GregorianCalendar();
        calendar.setTimeInMillis(instant.toEpochMilli());

        // Convert GregorianCalendar to XMLGregorianCalendar
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(calendar);
        } catch (DatatypeConfigurationException e) {
            LOG.error("Error creating XMLGregorianCalendar", e);
            throw new RuntimeException(e);
        }
    }

    public static Instant convertToInstant(XMLGregorianCalendar xmlGregorianCalendar) {
        if (xmlGregorianCalendar == null) {
            throw new IllegalArgumentException("XMLGregorianCalendar cannot be null");
        }
        // Convert XMLGregorianCalendar to GregorianCalendar
        GregorianCalendar calendar = xmlGregorianCalendar.toGregorianCalendar();

        // Convert GregorianCalendar to Instant
        return calendar.toInstant();
    }



    public static Duration parseTimeString(String timeString) {
        String[] parts = timeString.split(":");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid time string format. Expected HH:mm:ss");
        }

        int hours = Integer.parseInt(parts[0]);
        int minutes = Integer.parseInt(parts[1]);
        int seconds = Integer.parseInt(parts[2]);

        return Duration.ofHours(hours)
                .plusMinutes(minutes)
                .plusSeconds(seconds);
    }


    public static void waitSeconds(int timeoutSeconds, String waitForSubject) {
        waitMilliSeconds(timeoutSeconds * 1000, waitForSubject);
    }

    public static void waitMilliSeconds(long timeoutMilliSeconds, String waitForSubject) {
        try {
            Thread.sleep(timeoutMilliSeconds);
        } catch (InterruptedException e) {
            LOG.warn("Waiting for {} interrupted. Exiting...", waitForSubject);
            Thread.currentThread().interrupt();
        }
    }
}
