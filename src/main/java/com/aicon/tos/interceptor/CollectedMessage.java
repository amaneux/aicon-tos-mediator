package com.aicon.tos.interceptor;

import com.aicon.tos.connect.cdc.CDCAction;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

import static com.aicon.tos.interceptor.MessageMeta.TS_CDC_RECEIVED;
import static com.aicon.tos.interceptor.MessageMeta.TS_OFFSET;

/**
 * Represents a message collected in the CDC processing pipeline.
 * Stores metadata, values before and after processing, and change status.
 */
public class CollectedMessage {
    private static final Logger LOG = LoggerFactory.getLogger(CollectedMessage.class);

    protected List<InterceptorValueObject<?>> changedFields = null; // List of all changed values, will be generated at first call
    protected final Map<String, InterceptorValueObject<?>> fields;  // Map of fields having before/after values
    protected final MessageMeta meta;

    public CollectedMessage(CDCAction cdcAction, String entityName, long offset, long offsetTimestamp, String messageKey, Map<String, InterceptorValueObject<?>> fields) {
        meta = new MessageMeta(cdcAction, entityName, offset, offsetTimestamp, messageKey);
        this.fields = fields != null ? fields : new LinkedHashMap<>(0);
    }

    protected CollectedMessage(CollectedMessage other) {
        this.meta = other.meta;
        this.fields = other.fields;
    }

    /**
     * @return the acquired metadata about this message like timestamps, offset, etc.
     */
    public MessageMeta meta() {
        return meta;
    }

    public String getEntityName() {
        return meta.getEntityName();
    }

    public String getMessageKey() {
        return meta.getMessageKey();
    }

    public long getOffset() {
        return meta.getOffset();
    }

    public CDCAction getCDCAction() {
        return meta.getCDCAction();
    }

    public List<InterceptorValueObject<?>> getFields() {
        return fields == null
                ? Collections.emptyList()
                : fields.values().stream()
                .filter(Objects::nonNull) // Remove null elements from the list
                .collect(Collectors.toUnmodifiableList());
    }

    public InterceptorValueObject<?> getFieldValue(String fieldName) {
        if (fieldName == null) {
            LOG.warn("Field name is null for message with offset {}", getOffset());
            return null;
        }
        return fields.get(fieldName);
    }

    public String getFieldValueAsString(String fieldName) {
        InterceptorValueObject<?> field = getFieldValue(fieldName);
        if (field == null) {
            LOG.warn("Field {} not found in message with offset {}", fieldName, getOffset());
            return null;
        }
        return field.afterValue() == null ? null : field.afterValue().toString();
    }

    public Long getFieldValueAsLong(String fieldName, Long nullValue) {
        try {
            InterceptorValueObject<?> field = getFieldValue(fieldName);
            if (field == null) {
                LOG.warn("Field {} not found in message with offset {}", fieldName, getOffset());
                return nullValue;
            }
            return field.afterValue() != null ? (Long)field.afterValue() : nullValue;
        } catch (Exception e) {
            Object value = getFieldValue(fieldName).afterValue();
            LOG.warn("Field {}={} is not a Long (but a {}) in message with offset {}",
                    fieldName, value, value.getClass().getSimpleName(), getOffset());
        }
        return nullValue;
    }

    public Double getFieldValueAsDouble(String fieldName, Double nullValue) {
        try {
            InterceptorValueObject<?> field = getFieldValue(fieldName);
            if (field == null) {
                LOG.warn("Field {} not found in message with offset {}", fieldName, getOffset());
                return nullValue;
            }
            return field.afterValue() != null ? (Double)field.afterValue() : nullValue;
        } catch (Exception e) {
            Object value = getFieldValue(fieldName).afterValue();
            LOG.warn("Field {}={} is not a Double (but a {}) in message with offset {}",
                    fieldName, value, value.getClass().getSimpleName(), getOffset());
        }
        return nullValue;
    }

    /**
     * Safely compares the value of given field of this message against the same field in the other message, where any
     * null value will be taken care of (and typically results in false).
     * @param fieldName the field name to compare its value for
     * @param other the other message to compare the same field against
     * @return true when equal (also when both are null), else false.
     */
    public boolean compareFieldWithOther(String fieldName, FilteredMessage other) {
        if (fieldName == null || other == null) {
            return false;
        }
        return Objects.equals(getFieldValueAsString(fieldName), other.getFieldValueAsString(fieldName));
    }

    public List<InterceptorValueObject<?>> getChangedFields() {
        if (changedFields == null) {
            changedFields = fields.values().stream()
                    .filter(InterceptorValueObject::isChanged)
                    .toList();
        }
        return changedFields;
    }

    public boolean hasChanged(String fieldName) {
        return hasAllChanged(fieldName);
    }

    public boolean hasAllChanged(String... fieldNames) {
        boolean allChanged = true;
        for (String fieldName : fieldNames) {
            InterceptorValueObject<?> field = fields.get(fieldName);
            allChanged = allChanged && field != null && field.isChanged();
        }
        return allChanged;
    }

    /**
     * Builds a JSON payload based on `allFields`.
     *
     * @return JSON string representing the payload.
     */
    public String getPayloadAsJson() {
        Map<String, Object> fieldMap = new HashMap<>();
        for (InterceptorValueObject<?> field : fields.values()) {
            fieldMap.put(field.field().id(), field.afterValue());
        }

        ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.writeValueAsString(fieldMap);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize allFields to JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.format("%s: Entity=%s, Offset=%s, TS-Offset/Received=%s/%s, MsgKey=%s, CDCAction=%s, #changedFields=%s",
                this.getClass().getSimpleName(),
                meta.getEntityName(),
                meta.getOffset(),
                meta.getTimestampAsString(TS_OFFSET),
                meta.getTimestampAsString(TS_CDC_RECEIVED),
                meta.getMessageKey(),
                meta.getCDCAction(),
                getChangedFields().size()
        );
    }


    public String toStringExtended() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n").append(this.getClass().getSimpleName()).append(":\n");
        sb.append("  Entity            : ").append(meta.getEntityName()).append("\n");
        sb.append("  Offset            : ").append(meta.getOffset()).append("\n");
        sb.append("  TS Received/offset: ").append(meta.getTimestampAsString(TS_CDC_RECEIVED)).append(" / ").append(meta.getTimestampAsString(TS_OFFSET)).append("\n");
        sb.append("  Key               : ").append(meta.getMessageKey()).append("\n");
        sb.append("  CDCAction         : ").append(meta.getCDCAction()).append("\n");
        if (meta.getCDCAction() == CDCAction.CHANGED) {
            sb.append("  Changed Fields:\n");

            for (InterceptorValueObject<?> field : getChangedFields()) {
                sb.append(String.format("    Field: %-32s: %20s → %20s\n",
                        field.field().id(), field.beforeValue(), field.afterValue()));
            }
        }

        return sb.toString();
    }
}
