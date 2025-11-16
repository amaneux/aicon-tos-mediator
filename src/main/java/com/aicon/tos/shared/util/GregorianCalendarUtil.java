package com.aicon.tos.shared.util;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.util.GregorianCalendar;

public final class GregorianCalendarUtil {

    private GregorianCalendarUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static XMLGregorianCalendar getNowInXMLGregorianCalender() {
        long currentMillis = Instant.now().toEpochMilli();
        return getXMLGregorianCalender(currentMillis);
    }

    /**
     * Converts the specified timestamp into an XMLGregorianCalendar representation.
     * This method uses the provided moment (timestamp in milliseconds since epoch)
     * to create an XMLGregorianCalendar object by setting the time in a GregorianCalendar instance.
     *
     * @param moment the timestamp in milliseconds since epoch that will be converted
     *               into an XMLGregorianCalendar.
     * @return an instance of XMLGregorianCalendar representing the specified moment.
     */
    public static XMLGregorianCalendar getXMLGregorianCalender(long moment) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar();
        gregorianCalendar.setTimeInMillis(moment);
        return createXMLGregorianCalendar(gregorianCalendar);
    }

    /**
     * Creates an XMLGregorianCalendar instance from a given GregorianCalendar object.
     * This method converts the specified GregorianCalendar into its XMLGregorianCalendar
     * representation using a datatype factory.
     *
     * @param gregorianCalendar the GregorianCalendar to be converted
     * @return the XMLGregorianCalendar representation of the provided GregorianCalendar
     * @throws RuntimeException if there is an error configuring or creating the DatatypeFactory instance
     */
    public static XMLGregorianCalendar createXMLGregorianCalendar(GregorianCalendar gregorianCalendar) {
        try {
            DatatypeFactory datatypeFactory = DatatypeFactory.newInstance();
            return datatypeFactory.newXMLGregorianCalendar(gregorianCalendar);
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException("Error creating XMLGregorianCalendar", e);
        }
    }
}
