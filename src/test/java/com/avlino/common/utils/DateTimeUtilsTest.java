package com.avlino.common.utils;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import com.aicon.tos.control.OperatingModeRuleEngine;
import com.aicon.tos.shared.util.TimeSyncService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import util.InMemoryAppender;
import util.TimeSyncServiceTestHelper;

import javax.xml.datatype.XMLGregorianCalendar;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateTimeUtilsTest {

    @Test
    void testParseDateTime() {
        Instant instant1 = DateTimeUtils.parseZonedDateTime(
                "2025-03-21 11:39:04.345", ZoneId.systemDefault(), DateTimeUtils.DATE_TIME_MS_FORMAT);
        assertNotNull(instant1);
        assertTrue(instant1.toEpochMilli() > 0);

        Instant instant2 = DateTimeUtils.parseZonedDateTime(
                "2025-03-21 11:39:07.456", ZoneId.systemDefault(), DateTimeUtils.DATE_TIME_MS_FORMAT);
        assertNotNull(instant1);
        assertTrue(instant2.toEpochMilli() - instant1.toEpochMilli() == 3111);
    }
}