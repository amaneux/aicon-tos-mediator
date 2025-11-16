package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.http.JsonParser;
import com.aicon.tos.connect.web.pages.DataStore;
import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_value;
import com.avlino.aicon.ITVJobResequenceRequest.requests;
import com.avlino.aicon.ITVJobResequenceRequest.swaps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static com.aicon.tos.connect.flows.BaseController.splitReportLinesIfTooLong;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FlowControllerTest {

    private static final Logger LOG = LoggerFactory.getLogger(FlowControllerTest.class);
    private DataStore dataStore = DataStore.getInstance();

    @BeforeEach
    void setUp() {
        dataStore.setViewportWidth(800); // 100 characters x 8 pixels = 100 - 40 = 60 characters
        LOG.info("Viewport width set to {} characters", dataStore.getViewportWidth());
    }

    @Test
    void convertAvroSpecificRecordToJsonTest() {
        itv_job_resequence_request_value avroRecord = new itv_job_resequence_request_value();

        avroRecord.setTimestamp(Instant.ofEpochMilli(12345678));
        avroRecord.setAPIIdentifier(12345678);
        avroRecord.setRequestIdx("abc");
        avroRecord.setRequests(List.of());
        long currentTimestamp = System.currentTimeMillis();
        avroRecord.setTimestamp(Instant.ofEpochMilli(currentTimestamp));
        swaps swap = swaps.newBuilder()
                .setITVId("ITV-12345")
                .setSequenceNo1(1)
                .setCntrNo1("CNR-0001")
                .setCntrVisitId1("VISIT-001")
                .setBayIdxNo1("BAY-001")
                .setStackIdxNo1("STACK-001")
                .setTierIdxNo1("TIER-001")
                .setSequenceNo2(2)
                .setCntrNo2("CNR-0002")
                .setCntrVisitId2("VISIT-002")
                .setBayIdxNo2("BAY-002")
                .setStackIdxNo2("STACK-002")
                .setTierIdxNo2("TIER-002")
                .build();
        requests.Builder request = requests.newBuilder()
                .setQCId("QC53")
                .setSeqCount(2)
                .setSwaps(Collections.singletonList(swap))
                .setProgramId("interface")
                .setInternalIdx1("12345678")
                .setInternalIdx2("12345678")
                .setUptUserId("DGL");
        avroRecord.setRequests(Collections.singletonList(request.build()));

        String jsonOutput = JsonParser.convertAvroToJson(avroRecord);

        assertNotNull(jsonOutput, "JSON output should not be null");
        assertTrue(jsonOutput.contains("\"timestamp\":" + currentTimestamp),
                "JSON output should contain the timestamp as a long, not a formatted date");

        System.out.println("Converted JSON: " + jsonOutput);
    }

    @Test
    void insertNewlineAtMiddleAndIndent() {

        // Case 1: Input with a middle comma, split and indent
        String input1 = "FlowController example string to split, here is the continuation.";
        String expected1 = "FlowController example string to split,\n\there is the continuation.";
        assertEquals(expected1, splitReportLinesIfTooLong(input1));

        // Case 2: Input without proper split marker
        String input2 = "ThisIsAStringWithoutSplitPoint";
        String expected2 = "ThisIsAStringWithoutSplitPoint";
        assertEquals(expected2, splitReportLinesIfTooLong(input2));

        // Case 3: Very short string that doesn't meet splitting criteria
        String input3 = "Short";
        assertEquals(input3, splitReportLinesIfTooLong(input3));

        // Case 4: Multiple commas around the center
        String input4 = "Start part with information, split here nicely, end part1 continues, end part2 continues, end part3 continues, end part3 continues..";
        String expected4 = "Start part with information, split here nicely,\n\tend part1 continues, end part2 continues,\n\tend part3 continues, end part3 continues..";
        String actual = splitReportLinesIfTooLong(input4);
        LOG.info("Input:    {}", input4);
        LOG.info("Expected: {}", expected4);
        LOG.info("Actual:   {}", actual);
        assertEquals(expected4, actual);
    }

    @Test
    void insertNewlineAtMiddleAndIndent_withLongInput() {
        String input = "FlowController ControlFlow subscribed to AICON_TOS_CONTROL, threadState=RUNNABLE, endPoint=Http:TOS-N4, group.id = aicon-tos-mediator-controlflow\n" +
                "SessionsStats: #ACTIVE=0, DONE=2, FAILED=1\n" +
                "Last session finished @ 2025-Feb-18 10:56:57.691, State=FAILED, error";
        String expected = "FlowController ControlFlow subscribed to AICON_TOS_CONTROL,\n\tthreadState=RUNNABLE, endPoint=Http:TOS-N4,\n\tgroup.id = aicon-tos-mediator-controlflow\n" +
                "SessionsStats: #ACTIVE=0, DONE=2, FAILED=1\n" +
                "Last session finished @ 2025-Feb-18 10:56:57.691,\n\tState=FAILED, error";
        String actual = splitReportLinesIfTooLong(input);

        LOG.info("Input:    {}", input);
        LOG.info("Expected: {}", expected);
        LOG.info("Actual:   {}", actual);

        assertEquals(expected, actual);
    }

    @Test
    void insertNewlineAtMiddleAndIndent_withMultipleLongInputAndLeadingTabs() {
        String input = "FlowController ControlFlow subscribed to AICON_TOS_CONTROL, threadState=RUNNABLE, endPoint=Http:TOS-N4, group.id = aicon-tos-mediator-controlflow\n" +
                "\tSessionsStats: #ACTIVE=0, DONE=2, FAILED=1, Last session finished @ 2025-Feb-18 10:56:57.691, State=FAILED, error=Een hele langeeeeeeeeeeeeeeeeeeeeeeeeeeeeee regel";
        String expected = "FlowController ControlFlow subscribed to AICON_TOS_CONTROL,\n\tthreadState=RUNNABLE, endPoint=Http:TOS-N4,\n\tgroup.id = aicon-tos-mediator-controlflow\n" +
                "\tSessionsStats: #ACTIVE=0, DONE=2, FAILED=1,\n\t\tLast session finished @ 2025-Feb-18 10:56:57.691,\n\t\tState=FAILED,\n\t\terror=Een hele langeeeeeeeeeeeeeeeeeeeeeeeeeeeeee regel";
        String actual = splitReportLinesIfTooLong(input);

        LOG.info("Input:    {}", input);
        LOG.info("Expected: {}", expected);
        LOG.info("Actual:   {}", actual);

        assertEquals(expected, actual);
    }


    @Test
    void insertNewlineAtMiddleAndIndent_withMultipleVeryLongInputAndLeadingTabs() {
        String input = "\t\t\tLast session finished @ 2025-Feb-20 11:00:26.591, State=FAILED, " +
                "error=com.aicon.tos.connect.flows.ControlFlowSession@1957f976 failed, " +
                "reason: java.net.SocketTimeoutException: Connect timed out";


        String expected =  "\t\t\tLast session finished @ 2025-Feb-20 11:00:26.591,\n\t\t\t\tState=FAILED," +
                "\n\t\t\t\terror=com.aicon.tos.connect.flows.ControlFlowSession@1957f976 failed, " +
                "reason: java.net.SocketTimeoutException: Connect timed out";
        String actual = splitReportLinesIfTooLong(input);

        LOG.info("Input:    {}", input);
        LOG.info("Expected: {}", expected);
        LOG.info("Actual:   {}", actual);

        assertEquals(expected, actual);
    }
}
