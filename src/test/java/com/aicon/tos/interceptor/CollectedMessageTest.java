package com.aicon.tos.interceptor;

import com.avlino.common.MetaField;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static com.aicon.tos.connect.cdc.CDCAction.*;
import static com.aicon.tos.interceptor.MessageMeta.TS_CDC_RECEIVED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CollectedMessageTest {

    @Test
    void testConstructorWithListOfFields() {
        MetaField<String> field = new MetaField<>("status", String.class);
        InterceptorValueObject<String> vo = new InterceptorValueObject<>(field, "A", "B");

        CollectedMessage msg = new CollectedMessage(CHANGED, "test-entity", 0, 0, "123",
                Map.ofEntries(Map.entry(field.id(), vo)));

        assertEquals("test-entity", msg.getEntityName());
        assertEquals("123", msg.getMessageKey());
        assertEquals(1, msg.getFields().size());
        assertNotNull(msg.meta().getTimestamp(TS_CDC_RECEIVED));
        assertEquals(1, msg.getChangedFields().size());
    }

    @Test
    void testConstructorWithSingleField() {
        MetaField<Object> field = new MetaField<>("x", Object.class);
        CollectedMessage msg = new CollectedMessage(CHANGED, "entity-x", 0, 0, "key-x",
                Map.ofEntries(Map.entry(field.id(), new InterceptorValueObject<>(field, 1, 2))));

        assertEquals("entity-x", msg.getEntityName());
        assertEquals("key-x", msg.getMessageKey());
        assertEquals(1, msg.getFields().size());
        assertNotNull(msg.meta().getTimestamp(TS_CDC_RECEIVED));
    }

    @Test
    void testGetChangedFieldsFiltersUnchanged() {
        MetaField<String> field = new MetaField<>("unchanged", String.class);
        InterceptorValueObject<String> unchanged = new InterceptorValueObject<>(field, "same", "same");

        CollectedMessage msg = new CollectedMessage(CHANGED, "entity", 0, 0, "key",
                Map.ofEntries(Map.entry(field.id(), unchanged)));
        List<InterceptorValueObject<?>> changed = msg.getChangedFields();

        assertNotNull(changed);
        assertTrue(changed.isEmpty());
    }

    @Test
    void testToStringContainsValues() {
        MetaField<String> field = new MetaField<>("test", String.class);
        InterceptorValueObject<String> vo = new InterceptorValueObject<>(field, "before", "after");

        CollectedMessage msg = new CollectedMessage(CHANGED, "entity", 0, 0, "key",
                Map.ofEntries(Map.entry(field.id(), vo)));
        String str = msg.toString();

        assertTrue(str.contains("Entity=entity"));
        assertTrue(str.contains("Key=key"));
    }
}
