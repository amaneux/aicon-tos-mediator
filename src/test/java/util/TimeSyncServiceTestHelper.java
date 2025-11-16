package util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;

public class TimeSyncServiceTestHelper {

    /**
     * Converts a time string in the format "HH:mm:ss" into an {@link XMLGregorianCalendar}.
     *
     * @param timeString the time string to convert, e.g., "10:00:00".
     * @return the corresponding {@link XMLGregorianCalendar}.
     */
    public static XMLGregorianCalendar stringToXMLGregorianCalendar(String timeString) {
        // Parse the time string into a LocalTime
        LocalTime localTime = LocalTime.parse(timeString);

        // Use the current date with the parsed time
        ZonedDateTime zonedDateTime = localTime.atDate(java.time.LocalDate.now()).atZone(ZoneId.systemDefault());

        // Convert to GregorianCalendar
        GregorianCalendar gregorianCalendar = GregorianCalendar.from(zonedDateTime);

        // Convert to XMLGregorianCalendar
        try {
            return DatatypeFactory.newInstance().newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static Long xMLGregorianCalendarToMillis(XMLGregorianCalendar time) {
        if (time == null) {
            return null; // Handle null case
        }
        return time.toGregorianCalendar().getTimeInMillis();
    }
}
