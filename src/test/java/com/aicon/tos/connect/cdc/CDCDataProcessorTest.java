package com.aicon.tos.connect.cdc;

import com.aicon.TestConstants;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.jdom2.input.SAXBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CDCDataProcessorTest {

    private static Long FOURHOURSOLDTS = Instant.now().toEpochMilli() - 4 * 60 * 60 * 1000; // 4 hours old data
    private static String FOURHOURSOLDTSSTR = String.valueOf(FOURHOURSOLDTS);
    private Long TWODAYSOLDTS = Instant.now().toEpochMilli() - 48 * 60 * 60 * 1000; // 4 hours old data
    private String TWODAYSOLDTSSTR= String.valueOf(TWODAYSOLDTS);

    private CDCDataProcessor cdcDataProcessor;


    @BeforeEach
    void setUp() {
        String xml = "<CanaryCheck name=\"Canary Check\">\n" +
                "    <ConfigItem key=\"interval.s\" value=\"30\"/>\n" +
                "    <ConfigItem key=\"interval.cdc.s\" value=\"60\"/>\n" +
                "    <CDCTable name=\"table2\">\n" +
                "        <ConfigItem key=\"topic.name\" value=\"tos.TN4USER.INV_MOVE_EVENT_MV\"/>\n" +
                "        <ConfigItem key=\"group.id\" value=\"connect-mongo-sink-inv_move_event_mv\"/>\n" +
                "        <ConfigItem key=\"cdc.threshold\" value=\"1000\"/>\n" +
                "    </CDCTable>\n" +
                "    <CDCTable name=\"table1\">\n" +
                "        <ConfigItem key=\"topic.name\" value=\"tos.TN4USER.INV_UNIT_FCY_VISIT_MV\"/>\n" +
                "        <ConfigItem key=\"group.id\" value=\"connect-mongo-sink-INV_UNIT_FCY_VISIT_MV\"/>\n" +
                "        <ConfigItem key=\"cdc.threshold\" value=\"1500\"/>\n" +
                "    </CDCTable>\n" +
                "    <HttpRef ref=\"TOS-CANARY\"/>\n" +
                "</CanaryCheck>\n";

        SAXBuilder saxBuilder = new SAXBuilder();
        try {
            org.jdom2.Document document = saxBuilder.build(new java.io.StringReader(xml));
            org.jdom2.Element rootElement = document.getRootElement();

            ConfigSettings.setConfigFile(TestConstants.PATH_TO_TEST_CONFIG_FILES + "conf/mediator/cdcdataprocessortest.xml");
            ConfigGroup configGroup = new ConfigGroup(ConfigType.CanaryCheck);
            configGroup.fromXml(rootElement);

            cdcDataProcessor = new CDCDataProcessor(configGroup);

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Error parsing XML", e);
        }

    }

//    @Test
//    void testGetCDCConfigMap() {
//        // Verify the threshold map is correctly populated
//        Map<String, Long> thresholdMap = cdcDataProcessor.getCDCConfigMap("table1:1000,table2:2000");
//        assertEquals(2, thresholdMap.size());
//        assertEquals(1000L, thresholdMap.get("table1"));
//        assertEquals(2000L, thresholdMap.get("table2"));
//    }

    @Test
    void testProcessCDCData_MatchingThreshold() {
        // Mock CDC data
        List<CDCData> tosData = Arrays.asList(
                new CDCData("table1", "1", 100L),
                new CDCData("table2", "2", 200L)
        );

        List<CDCData> aiconData = Arrays.asList(
                new CDCData("table1", "1", 150L),  // Matches within threshold
                new CDCData("table2", "2", 300L)  // Matches within threshold
        );

        assertTrue(cdcDataProcessor.processCDCData(tosData, aiconData));
    }

    @Test
    void testProcessCDCData_ExceedsThreshold() {
        // Mock CDC data
        List<CDCData> tosData = Arrays.asList(
                new CDCData("table1", "1", 100L),
                new CDCData("table2", "2", 200L)
        );

        List<CDCData> aiconData = Arrays.asList(
                new CDCData("table1", "1", 5000L), // Exceeds threshold
                new CDCData("table2", "2", 300L)  // Matches within threshold
        );

        assertFalse(cdcDataProcessor.processCDCData(tosData, aiconData));
    }

    @Test
    void testConvertStringToCDCData() {
        CharSequence cdcInfo = "table1=1:" + FOURHOURSOLDTSSTR + ",table2=2:" + FOURHOURSOLDTSSTR;
        List<CDCData> result = cdcDataProcessor.convertStringToCDCData(cdcInfo);
        assertEquals(2, result.size());
        assertEquals("table1", result.get(0).getTableName());
        assertEquals("1", result.get(0).getGkey());
        assertEquals(FOURHOURSOLDTS, result.get(0).getCreationTimestamp());
        assertEquals("table2", result.get(1).getTableName());
        assertEquals("2", result.get(1).getGkey());
        assertEquals(FOURHOURSOLDTS, result.get(1).getCreationTimestamp());
    }

    @Test
    void testDetermineCdcStatus_AllDataMatching() {
        String cdcTables = "table1=1:" + FOURHOURSOLDTSSTR + ",table2=2:" + FOURHOURSOLDTSSTR;
        assertTrue(cdcDataProcessor.determineCdcStatus
                (cdcTables, cdcDataProcessor.getSimulatedAiconCDCData(cdcTables, 0)));
    }


    @Test
    void testDetermineCdcStatus_DatedDataMatching() {
        String cdcTables = "table1=1:" + TWODAYSOLDTSSTR + ",table2=2:" + FOURHOURSOLDTSSTR;
        assertFalse(cdcDataProcessor.determineCdcStatus
                (cdcTables, cdcDataProcessor.getSimulatedAiconCDCData(cdcTables, 0)));
    }

    @Test
    void testDetermineCdcStatus_EmptyData() {
        String cdcTables = "";
        assertTrue(cdcDataProcessor.determineCdcStatus
                (cdcTables, cdcDataProcessor.getSimulatedAiconCDCData(cdcTables, 0)));
    }
}
