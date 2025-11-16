package com.aicon.tos.interceptor.newgenproducerconsumer.mock;

import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.interceptor.InterceptorConfig;
import com.aicon.tos.shared.connectors.ConnectorProgress;
import com.aicon.tos.shared.util.AnsiColor;
import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.aicon.tos.interceptor.decide.scenarios.n4.events.WiMoveKindEnum.VESL_DISCH;
import static com.aicon.tos.shared.util.TimeUtils.waitMilliSeconds;

public class MockInterceptorConsumer implements InterceptorConsumerInterface {

    private static final Logger LOG = LoggerFactory.getLogger(MockInterceptorConsumer.class);

    private final String topic;
    private final ConnectorProgress status = new ConnectorProgress();
    private final Schema keySchema;
    private final Schema envelopeSchema;
    private final Schema afterSchema;
    private final Schema beforeSchema;
    DataStore dataStore = DataStore.getInstance();

    private CompassDirection getNextCompassDirection() {
        CompassDirection next = currentCompassDirection.next();
        currentCompassDirection = next;
        return next;
    }

    private CompassDirection getCompassDirection() {
        CompassDirection next = currentCompassDirection.next();
        currentCompassDirection = next;
        return next;
    }

    private static final String T_WI = "inv_wi";
    private static final String T_TEST = "inv_test";
    private static final String T_RTT = "road_truck_transactions";
    //    private static final List<String> topicsTurn = List.of(T_RTT, T_WI, T_WI, T_RTT); //for gate scenario
    private static final List<String> topicsTurn = List.of(T_WI, T_WI); //for vessel discharge scenario

    private static Integer messageTurn = -1;
    private static final int messageDelay = 15000;
    private static Instant messageSendTs = Instant.now();
    private static boolean messageSend = false;

    List<ConsumerRecord<GenericRecord, GenericRecord>> inv_wi_records = new ArrayList<>();
    List<ConsumerRecord<GenericRecord, GenericRecord>> inv_test_records = new ArrayList<>();
    List<ConsumerRecord<GenericRecord, GenericRecord>> rtt_records = new ArrayList<>();
    private int max_inv_wi_records = 2;
    private int max_inv_test_records = 2;
    private int max_rtt_records = 2;
    private int current_inv_wi_record = 0;
    private int current_inv_test_record = 0;
    private int current_rtt_record = 0;

    private final Object consumerLock = new Object(); // Lock for thread-safe access

    public static void main(String[] args) {
        MockInterceptorConsumer mockInterceptorConsumer = new MockInterceptorConsumer(T_WI);
        while (true) {
            mockInterceptorConsumer.pollMessages();
        }
    }

    private InterceptorConfig interceptorConfig = new InterceptorConfig();

    enum CompassDirection {
        NORTH, EAST, SOUTH, WEST;

        public CompassDirection next() {
            switch (this) {
                case NORTH:
                    return EAST;
                case EAST:
                    return SOUTH;
                case SOUTH:
                    return WEST;
                case WEST:
                    return NORTH;
                default:
                    throw new AssertionError("Unknown direction: " + this);
            }
        }
    }

    private CompassDirection currentCompassDirection = CompassDirection.NORTH;

    public MockInterceptorConsumer(String topic) {
        this.topic = topic;
        dataStore.addMockedConsumer(this);
        status.setProgress(ConnectorProgress.ConnectorState.INITIALISING);
        this.envelopeSchema = interceptorConfig.getSchema(topic);
        this.keySchema = envelopeSchema.getField("Key").schema().getTypes().stream()
                .filter(s -> s.getType() == Schema.Type.RECORD).findFirst()
                .orElseThrow(() -> new RuntimeException("No record type found for 'Key' field"));
        this.afterSchema = envelopeSchema.getField("after").schema().getTypes().stream()
                .filter(s -> s.getType() == Schema.Type.RECORD).findFirst()
                .orElseThrow(() -> new RuntimeException("No record type found for 'after' field"));
        this.beforeSchema = envelopeSchema.getField("before").schema().getTypes().stream()
                .filter(s -> s.getType() == Schema.Type.RECORD).findFirst()
                .orElseThrow(() -> new RuntimeException("No record type found for 'before' field"));
        this.setupRecords();
        status.setProgress(ConnectorProgress.ConnectorState.INITIALIZED);
    }

    private void setupRecords() {
        if (topic.equals(T_WI)) setUpInvWiRecords();
//        if (topic.equals(T_TEST)) setUpInvTestRecords();
//        if (topic.equals(T_RTT)) setUpRttRecords();
    }

    private void setUpInvWiRecords() {

        //-=================Gate Arrival Records========================
        GenericRecord beforeRecord = new GenericData.Record(beforeSchema);
//        setField(beforeRecord, "gkey", "19610221");
//        setField(beforeRecord, "facility_gkey", 1);
//        setField(beforeRecord, "door_direction", getCompassDirection());
//        setField(beforeRecord, "suspend_state", 1);
//        setField(beforeRecord, "move_kind", "RECV");
//        setField(beforeRecord, "truck_visit_ref", "998899889988");
//
        GenericRecord afterRecord = new GenericData.Record(afterSchema);
//        setField(afterRecord, "gkey", "19610221");
//        setField(afterRecord, "facility_gkey", 1);
//        setField(afterRecord, "door_direction", getNextCompassDirection());
//        setField(afterRecord, "suspend_state", 1);
//        setField(afterRecord, "move_kind", "RECV");
//        setField(afterRecord, "truck_visit_ref", "998899889988");
//
        GenericRecord envelopeRecord = new GenericData.Record(envelopeSchema);
//        setField(envelopeRecord, "before", beforeRecord);
//        setField(envelopeRecord, "after", afterRecord);
//        setField(envelopeRecord, "op", "c");
//        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());
//
        GenericRecord keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "key-0");

        ConsumerRecord<GenericRecord, GenericRecord> consumerRecord = new ConsumerRecord<>(topic, 0, 0, keyRecord, envelopeRecord);
//        inv_wi_records.add(consumerRecord);
//
//        beforeRecord = new GenericData.Record(beforeSchema);
//        setField(beforeRecord, "gkey", "19610222");
//        setField(beforeRecord, "facility_gkey", 1);
//        setField(beforeRecord, "door_direction", getCompassDirection());
//        setField(beforeRecord, "suspend_state", 1);
//        setField(beforeRecord, "move_kind", "RECV");
//        setField(beforeRecord, "truck_visit_ref", "112211221122");
//
//        afterRecord = new GenericData.Record(afterSchema);
//        setField(afterRecord, "gkey", "19610222");
//        setField(afterRecord, "facility_gkey", 1);
//        setField(afterRecord, "door_direction", getNextCompassDirection());
//        setField(afterRecord, "suspend_state", 1);
//        setField(afterRecord, "move_kind", "RECV");
//        setField(afterRecord, "truck_visit_ref", "112211221122");
//
//        envelopeRecord = new GenericData.Record(envelopeSchema);
//        setField(envelopeRecord, "before", beforeRecord);
//        setField(envelopeRecord, "after", afterRecord);
//        setField(envelopeRecord, "op", "c");
//        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());
//
//        consumerRecord = new ConsumerRecord<>(topic, 0, 1, "key-1", envelopeRecord);
//        inv_wi_records.add(consumerRecord);

        //-=================Vessel Discharge Records========================

        keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "18961567");
        beforeRecord = new GenericData.Record(beforeSchema);
        setField(beforeRecord, "gkey", "18961567");
        setField(beforeRecord, "facility_gkey", 1);
        setField(beforeRecord, "door_direction", getCompassDirection());
        setField(beforeRecord, "suspend_state", 1);
        setField(beforeRecord, "move_kind", VESL_DISCH.getTosCode());
        setField(beforeRecord, "itv_gkey", "330033003300");
        setField(beforeRecord, "pos_slot", "B3C1R2");

        afterRecord = new GenericData.Record(afterSchema);
        setField(afterRecord, "gkey", "18961567");
        setField(afterRecord, "facility_gkey", 1);
        setField(afterRecord, "door_direction", getNextCompassDirection());
        setField(afterRecord, "suspend_state", 1);
        setField(afterRecord, "move_kind", VESL_DISCH.getTosCode());
        setField(afterRecord, "itv_gkey", "330033003300");
        setField(afterRecord, "pos_slot", "B3C1R2");

        envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        consumerRecord = new ConsumerRecord<>(topic, 0, 1, keyRecord, envelopeRecord);
        inv_wi_records.add(consumerRecord);

        keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "18961566");
        beforeRecord = new GenericData.Record(beforeSchema);
        setField(beforeRecord, "gkey", "18961566");
        setField(beforeRecord, "facility_gkey", 1);
        setField(beforeRecord, "door_direction", getCompassDirection());
        setField(beforeRecord, "suspend_state", 1);
        setField(beforeRecord, "move_kind", VESL_DISCH.getTosCode());
        setField(beforeRecord, "itv_gkey", "330033003300");
        setField(beforeRecord, "pos_slot", "B3C1R1");

        afterRecord = new GenericData.Record(afterSchema);
        setField(afterRecord, "gkey", "18961566");
        setField(afterRecord, "facility_gkey", 1);
        setField(afterRecord, "door_direction", getNextCompassDirection());
        setField(afterRecord, "suspend_state", 1);
        setField(afterRecord, "move_kind", VESL_DISCH.getTosCode());
        setField(afterRecord, "itv_gkey", "330033003300");
        setField(afterRecord, "pos_slot", "B3C1R1");

        envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        consumerRecord = new ConsumerRecord<>(topic, 0, 1, keyRecord, envelopeRecord);
        inv_wi_records.add(consumerRecord);
    }

    private void setUpInvTestRecords() {
        GenericRecord keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "key-0");
        GenericRecord beforeRecord = new GenericData.Record(beforeSchema);
        setField(beforeRecord, "facility_gkey", 1);

        GenericRecord afterRecord = new GenericData.Record(afterSchema);
        setField(afterRecord, "facility_gkey", 1);

        GenericRecord envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        ConsumerRecord<GenericRecord, GenericRecord> consumerRecord = new ConsumerRecord<>(topic, 0, 0, keyRecord, envelopeRecord);
        inv_test_records.add(consumerRecord);
    }

    private void setUpRttRecords() {
        GenericRecord keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "key-0");
        GenericRecord beforeRecord = new GenericData.Record(beforeSchema);

        GenericRecord afterRecord = new GenericData.Record(afterSchema);
        setField(afterRecord, "gkey", "998899889988");

        GenericRecord envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        ConsumerRecord<GenericRecord, GenericRecord> consumerRecord = new ConsumerRecord<>(topic, 0, 0, keyRecord, envelopeRecord);
        rtt_records.add(consumerRecord);

        keyRecord = new GenericData.Record(keySchema);
        keyRecord.put("gkey", "112211221122");
        beforeRecord = new GenericData.Record(beforeSchema);
        afterRecord = new GenericData.Record(afterSchema);
        setField(afterRecord, "gkey", "112211221122");

        envelopeRecord = new GenericData.Record(envelopeSchema);
        setField(envelopeRecord, "before", beforeRecord);
        setField(envelopeRecord, "after", afterRecord);
        setField(envelopeRecord, "op", "c");
        setField(envelopeRecord, "ts_ms", System.currentTimeMillis());

        consumerRecord = new ConsumerRecord<>(topic, 0, 1, keyRecord, envelopeRecord);
        rtt_records.add(consumerRecord);
    }


    private void nextMessageOrNot() {
        synchronized (consumerLock) {
            Instant now = Instant.now();
            if (messageSend &&
                    messageTurn < topicsTurn.size() &&
                    now.isAfter(messageSendTs.plusMillis(messageDelay))) {
                messageSendTs = now;
                messageTurn++;
                messageSend = false;
                if (messageTurn < topicsTurn.size() && LOG.isInfoEnabled()) {
                    LOG.info(AnsiColor.brightGreen("next topic to send a message is {}"), topicsTurn.get(messageTurn));
                }
            }
        }
    }


    @Override
    public ConsumerRecords<GenericRecord, GenericRecord> pollMessages() {
        status.setProgress(ConnectorProgress.ConnectorState.CONNECTED);

        nextMessageOrNot();

        waitMilliSeconds(interceptorConfig.getEntityConfig(topic).getProcessingDelay(), "");

        ConsumerRecord<GenericRecord, GenericRecord> record = null;

        if (Objects.equals(topic, "inv_wi"))
            record = getTopicRecord_inv_wi();
        if (Objects.equals(topic, "inv_test"))
            record = getTopicRecord_inv_test();
        if (Objects.equals(topic, "road_truck_transactions"))
            record = getTopicRecord_road_truck_transactions();

        Map<TopicPartition, List<ConsumerRecord<GenericRecord, GenericRecord>>> recordsMap = new HashMap<>();
        if (record != null) {
            recordsMap.put(new TopicPartition(topic, 0), List.of(record));
        }
        return new ConsumerRecords<>(recordsMap);
    }

    private ConsumerRecord<GenericRecord, GenericRecord> getTopicRecord_inv_wi() {
        if (!messageSend) {
            messageSend = true;
            if (current_inv_wi_record >= max_inv_wi_records) {
                return null;
            }
            return inv_wi_records.get(current_inv_wi_record++);
        }
        return null;
    }


    private ConsumerRecord<GenericRecord, GenericRecord> getTopicRecord_inv_test() {
        if (!messageSend) {
            messageSend = true;
            if (current_inv_test_record >= max_inv_test_records) {
                return null;
            }
            return inv_test_records.get(current_inv_test_record++);
        }
        return null;
    }

    private ConsumerRecord<GenericRecord, GenericRecord> getTopicRecord_road_truck_transactions() {
        if (!messageSend) {
            messageSend = true;
            if (current_rtt_record >= max_rtt_records) {
                return null;
            }
            return rtt_records.get(current_rtt_record++);
        }
        return null;
    }

    private void setField(GenericRecord record, String fieldName, Object value) {

        if (record.getSchema().getField(fieldName) != null) {
            record.put(fieldName, value);
        } else {
            LOG.error("Field {} not found in schema: {}", fieldName, record.getSchema().getName());
            System.exit(1);
//            throw new FieldNotFoundException(fieldName, record.getSchema().getName());
        }
    }

//    private String getNextCompassDirection() {
//        currentCompassDirection = currentCompassDirection.next();
//        return currentCompassDirection.name();
//    }
//
//    private String getCompassDirection() {
//        return currentCompassDirection.name();
//    }

    @Override
    public void close() {
        // Nothing to close for the mock
    }

    @Override
    public String getTopic() {
        return this.topic;
    }

    @Override
    public ConnectorProgress getStatus() {
        return status;
    }
}