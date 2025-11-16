package com.aicon.tos.interceptor;

import com.aicon.tos.connect.cdc.CDCAction;
import com.aicon.tos.shared.ResultLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MessageMetaTest {

    @Test
    void testSetResultWhenHigher() {
        MessageMeta meta = new MessageMeta(CDCAction.CHANGED, "test-entity", 1, System.currentTimeMillis() - 1000, "key-1");

        String text = meta.setResultWhenHigher(ResultLevel.ERROR, "simple text");
        assertEquals("simple text", text);

        text = meta.setResultWhenHigher(ResultLevel.ERROR, "simple text with p1={} and p2={}", "val1", 3);
        String lastErrorText = "simple text with p1=val1 and p2=3";
        assertEquals(lastErrorText, text);
        assertEquals(ResultLevel.ERROR, meta.getResult().getLevel());
        assertEquals(text, meta.getResult().getMessage());

        text = meta.setResultWhenHigher(ResultLevel.OK, null);                  // OK should not override ERROR
        assertNull(text);
        assertEquals(ResultLevel.ERROR, meta.getResult().getLevel());
        assertEquals(lastErrorText, meta.getResult().getMessage());

        text = meta.setResultWhenHigher(ResultLevel.WARN, "any other text");    // same for WARN
        assertEquals("any other text", text);
        assertEquals(ResultLevel.ERROR, meta.getResult().getLevel());
        assertEquals(lastErrorText, meta.getResult().getMessage());

        text = meta.setResultWhenHigher(ResultLevel.ERROR, "any other error");  // but it will override any other ERROR text
        assertEquals("any other error", text);
        assertEquals(ResultLevel.ERROR, meta.getResult().getLevel());
        assertEquals("any other error", meta.getResult().getMessage());
    }
}
