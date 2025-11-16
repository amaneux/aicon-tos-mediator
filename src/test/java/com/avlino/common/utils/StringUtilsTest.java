package com.avlino.common.utils;

import org.junit.jupiter.api.Test;

import static com.avlino.common.utils.StringUtils.*;
import static org.junit.jupiter.api.Assertions.*;

class StringUtilsTest {

    @Test
    void testFormat() {
        assertNull(format(null));
        assertNull(format(null, null));
        assertNull(format(null, "p1"));
        assertEquals("Some text", format("Some text", "p1"));
        assertEquals("Some text {}", format("Some text {}", null));
        assertEquals("Some text p1", format("Some text {}", "p1"));
        assertEquals("Some text p1", format("Some text {}", "p1", null));
        assertEquals("Some text p1", format("Some text {}", "p1", "p2"));
        assertEquals("Some text p1, p2", format("Some text {}, {}", "p1", "p2"));
        assertEquals("Some text p1, p2 and more {}", format("Some text {}, {} and more {}", "p1", "p2"));
        assertEquals("Some text p1, p2 and more {p3}", format("Some text {}, {} and more {{}}", "p1", "p2", "p3"));
        assertEquals("Some text p1, p2 and more {p3} + p4", format("Some text {}, {} and more {{}} + {}", new String[] {"p1", "p2", "p3", "p4"}));
    }

    @Test
    void testHasContent() {
        assertFalse(hasContent(null));
        assertFalse(hasContent(""));
        assertTrue(hasContent(" "));
    }

    @Test
    void testIfNull() {
        assertNull(ifNull(null, null));
        assertEquals("", ifNull(null, ""));
        assertEquals("some", ifNull("some", ""));
    }

    @Test
    void testConcat() {
        assertNull(concat(null, null, PIPE));
        assertEquals("some", concat("some", null, PIPE));
        assertEquals("some", concat(null, "some", PIPE));
        assertEquals("some|other", concat("some", "other", PIPE));
        assertEquals("some|other|thing", concat("some|other", "thing", PIPE));
        assertEquals("some|other | thing", concat("some|other", "thing", " " + PIPE + " "));

        assertEquals("one,more,thing", concatCsv("one,more", "thing"));
    }
}
