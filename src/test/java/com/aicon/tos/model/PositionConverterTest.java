package com.aicon.tos.model;

import com.aicon.TestConstants;
import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.avlino.common.KeyValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.aicon.tos.model.PositionConverter.CFG_GRP_CONVERSIONS;
import static com.aicon.tos.model.PositionConverter.KEY_CONV_BLOCK;
import static com.aicon.tos.model.PositionConverter.KEY_CONV_COLUMN;
import static com.aicon.tos.model.PositionConverter.KEY_CONV_ROW;
import static com.aicon.tos.model.PositionConverter.KEY_CONV_TIER;
import static com.aicon.tos.model.PositionConverter.POS_KEY_SEP;
import static com.aicon.tos.model.PositionConverter.POS_PART_BLOCK;
import static com.aicon.tos.model.PositionConverter.POS_PART_COLUMN;
import static com.aicon.tos.model.PositionConverter.POS_PART_ROW;
import static com.aicon.tos.model.PositionConverter.POS_PART_SEP;
import static com.aicon.tos.model.PositionConverter.POS_PART_TIER;
import static com.aicon.tos.model.PositionConverter.POS_PART_TYPE;
import static com.aicon.tos.model.PositionConverter.POS_PART_YARD;
import static com.aicon.tos.model.PositionConverter.UNKNOWN_YARD_ID;
import static com.aicon.tos.model.PositionConverter._conversionGrp;
import static com.aicon.tos.model.PositionConverter._posTos2Aicon;
import static com.aicon.tos.model.PositionConverter.collectParts;
import static com.aicon.tos.model.PositionConverter.convertPosPartsToAicon;
import static com.aicon.tos.model.PositionConverter.convertPosPartsToTos;
import static com.aicon.tos.model.PositionConverter.mergePosition;
import static com.aicon.tos.model.PositionConverter.splitPosition;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class to convert the logic of the PositionConverter class without reading from the config file.
 */
public class PositionConverterTest {


    @BeforeEach
    void setup() {
        // Set the test configuration file path before each test
        ConfigSettings.setConfigFile(TestConstants.TEST_CONFIG_FULL_FILESPEC);
    }

    @Test
    public void getPositionSchema() {
        List<KeyValue> parts = PositionConverter.getPositionSchema("B3R2C1T1");
        assertNotNull(parts);
        assertEquals(4, parts.size());
        assertEquals(POS_PART_BLOCK, parts.get(0).key());
        assertEquals(3, parts.get(0).value());
        assertEquals(POS_PART_ROW, parts.get(1).key());
        assertEquals(2, parts.get(1).value());
        assertEquals(POS_PART_COLUMN, parts.get(2).key());
        assertEquals(1, parts.get(2).value());
        assertEquals(POS_PART_TIER, parts.get(3).key());
        assertEquals(1, parts.get(3).value());

        parts = PositionConverter.getPositionSchema("B4C2R3.T1");
        assertNotNull(parts);
        assertEquals(5, parts.size());
        assertEquals(POS_PART_BLOCK, parts.get(0).key());
        assertEquals(4, parts.get(0).value());
        assertEquals(POS_PART_COLUMN, parts.get(1).key());
        assertEquals(2, parts.get(1).value());
        assertEquals(POS_PART_ROW, parts.get(2).key());
        assertEquals(3, parts.get(2).value());
        assertEquals(POS_KEY_SEP, parts.get(3).key());
        assertEquals(".", parts.get(3).value());
        assertEquals(POS_PART_TIER, parts.get(4).key());
        assertEquals(1, parts.get(4).value());

        parts = PositionConverter.getPositionSchema("B3R3");
        assertNotNull(parts);
        assertEquals(2, parts.size());
        assertEquals(POS_PART_BLOCK, parts.get(0).key());
        assertEquals(3, parts.get(0).value());
        assertEquals(POS_PART_ROW, parts.get(1).key());
        assertEquals(3, parts.get(1).value());
    }

    @Test
    public void testSplitPosition() {
        // Schema & position without tier
        PositionConverter.getPositionSchema("B3C1R2");
        LinkedHashMap<String,String> parts = splitPosition("M01C07");
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));

        // Ignore that position has a tier, while schema not.
        parts = splitPosition("M01C07.3");
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));

        // With tier added to the schema.
        PositionConverter.getPositionSchema("B3C1R2.T1");
        parts = splitPosition("M01C07.3");
        assertNotNull(parts);
        assertEquals(4, parts.size());
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));
        assertEquals("3", parts.get(POS_PART_TIER));

        // Should accept missing tier
        parts = splitPosition("M01C07");
        assertNotNull(parts);
        assertEquals(3, parts.size());
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));

        // Parse full position description including yard
        parts = splitPosition("Y-RON-M01C07");
        assertNotNull(parts);
        assertEquals(5, parts.size());
        assertEquals("Y", parts.get(POS_PART_TYPE));
        assertEquals("RON", parts.get(POS_PART_YARD));
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));

        // Parse full position description including yard, all separated (with : as well this time)
        parts = splitPosition("Y-RON:M01-C-07-5");
        assertNotNull(parts);
        assertEquals(6, parts.size());
        assertEquals("Y", parts.get(POS_PART_TYPE));
        assertEquals("RON", parts.get(POS_PART_YARD));
        assertEquals("M01", parts.get(POS_PART_BLOCK));
        assertEquals("C", parts.get(POS_PART_COLUMN));
        assertEquals("07", parts.get(POS_PART_ROW));
        assertEquals("5", parts.get(POS_PART_TIER));

        // Error situations

        // Length of position shorter than its schema
        try {
            parts = splitPosition("M0107");
            fail("Should have thrown an exception for bad position");
        } catch (StringIndexOutOfBoundsException e) {
            // as expected
        }

        // No position given
        try {
            parts = splitPosition(null);
            fail("Should have thrown an exception for null position");
        } catch (StringIndexOutOfBoundsException e) {
            // as expected
        }
    }

    @Test
    public void testMergePosition() {
        String yardId = ConfigDomain.getScopePart(ConfigDomain.SCOPE_ID_YARD);

        PositionConverter.getPositionSchema("B3R3C1.T1");
        String position = mergePosition(collectParts("01A", "117", "5", "3"), false, false);
        assertEquals("01A1175.3", position);

        position = mergePosition(collectParts("01A", "117", "5", "3"), true, false);
        assertEquals(String.format("Y-%s-01A1175.3", yardId), position);

        position = mergePosition(collectParts("01A", "117", "5", "3"), true, true);
        assertEquals(String.format("Y-%s-01A-117-5-3", yardId), position);

        PositionConverter.getPositionSchema("B4C1R2");
        position = mergePosition(collectParts("M008", "16", "D", "A"), false, false);
        assertEquals("M008D16", position);

        // and now a non-sense schema just to show that it can mix everything it needs to.
        PositionConverter.getPositionSchema("T4.R3:C2/B1");
        position = mergePosition(collectParts("B", "ROW", "CL", "TIER"), false, false);
        assertEquals("TIER.ROW:CL/B", position);

        position = mergePosition(collectParts("B", "ROW", "CL", "TIER"), true, true);
        assertEquals(String.format("Y-%s-TIER-ROW-CL-B", yardId), position);

        position = mergePosition(collectParts(null, null, "CL", "TIER"), true, true);
        assertEquals(String.format("Y-%s-TIER-null-CL-null", yardId), position);

        PositionConverter.setPositionSep("/");
        position = mergePosition(collectParts("B", "ROW", "CL", "TIER"), true, true);
        assertEquals(String.format("Y/%s/TIER/ROW/CL/B", yardId), position);
        PositionConverter.setPositionSep(POS_PART_SEP);     // to make other test repeatable
    }

    @Test
    public void testConversions() {
        // Next convert a TOS pos when nothing is configured (expect pos parts to be passed as is).
        _conversionGrp = null;      // reset previous configuration, it will try to look up the SqlDb config which is not there.
        _posTos2Aicon = null;       // and make sure it will reprocess the conversion mappings (which are not there this time)

        PositionConverter.getPositionSchema("B3R2C1.T1");
        ConfigGroup convGrp = new ConfigGroup(ConfigType.ConfigGroup, CFG_GRP_CONVERSIONS);
        convGrp.addItem(new ConfigItem(KEY_CONV_BLOCK, "01A -> 1101, 01B -> 1102, 01C -> 1103, 01D -> 1104, 01E -> 1105"));
        convGrp.addItem(new ConfigItem(KEY_CONV_ROW, "-1/2"));
        convGrp.addItem(new ConfigItem(KEY_CONV_COLUMN, "A->0,B->1,C->2,D->3,E->4,F->5,G->6,H->7,J->8,K->9,L->10,M->11"));
        convGrp.addItem(new ConfigItem(KEY_CONV_TIER, "-1"));
        _conversionGrp = convGrp;

        // First convert a TOS pos to its aicon equivalents
        String tosPos = "01C05D.2";
        LinkedHashMap<String, String> tosMap = splitPosition(tosPos);
        Map<String, String> aiconMap = convertPosPartsToAicon(tosMap);
        assertNotNull(aiconMap);
        assertEquals(4, aiconMap.size());
        assertEquals("1103", aiconMap.get(POS_PART_BLOCK));
        assertEquals("2", aiconMap.get(POS_PART_ROW));
        assertEquals("3", aiconMap.get(POS_PART_COLUMN));
        assertEquals("1", aiconMap.get(POS_PART_TIER));

        // so and now back to tos parts and merge them together to check them with the original position.
        Map<String, String> tosMap2 = convertPosPartsToTos(aiconMap);
        String tosPos2 = mergePosition(collectParts(
                tosMap2.get(POS_PART_BLOCK),
                tosMap2.get(POS_PART_ROW),
                tosMap2.get(POS_PART_COLUMN),
                tosMap2.get(POS_PART_TIER)),
                false, false
        );
        // verify that position after conversion to Aicon and back to TOS results in same starting position.
        assertEquals(tosPos, tosPos2);

        // Next convert a TOS pos when nothing is configured (expect pos parts to be passed as is).
        _conversionGrp = null;      // reset previous configuration, it will try to look up the SqlDb config which is not there.
        _posTos2Aicon = null;       // and make sure it will reprocess the conversion mappings (which are not there this time)

        tosMap = splitPosition(tosPos);
        aiconMap = convertPosPartsToAicon(tosMap);
        assertNotNull(aiconMap);
        assertEquals(4, aiconMap.size());
        // Expect all 4 parts 1 on 1 from the given tosPos.
        assertEquals("01C", aiconMap.get(POS_PART_BLOCK));
        assertEquals("05", aiconMap.get(POS_PART_ROW));
        assertEquals("D", aiconMap.get(POS_PART_COLUMN));
        assertEquals("2", aiconMap.get(POS_PART_TIER));

        // so and now back to tos parts and merge them together to check them with the original position.
        tosMap2 = convertPosPartsToTos(aiconMap);
        tosPos2 = mergePosition(collectParts(
                        tosMap2.get(POS_PART_BLOCK),
                        tosMap2.get(POS_PART_ROW),
                        tosMap2.get(POS_PART_COLUMN),
                        tosMap2.get(POS_PART_TIER)),
                false, false
        );
        // verify that position after conversion to Aicon and back to TOS results in same starting position.
        assertEquals(tosPos, tosPos2);

    }

    @Test
    public void testConversionsLBCT() {
        // Next convert a TOS pos when nothing is configured (expect pos parts to be passed as is).
        _conversionGrp = null;      // reset previous configuration, it will try to look up the SqlDb config which is not there.
        _posTos2Aicon = null;       // and make sure it will reprocess the conversion mappings (which are not there this time)

        PositionConverter.getPositionSchema("B3R2C2.T1");
        ConfigGroup convGrp = new ConfigGroup(ConfigType.ConfigGroup, CFG_GRP_CONVERSIONS);
        convGrp.addItem(new ConfigItem(KEY_CONV_BLOCK, "A01->1101,A02->1102,A03->1103,A04->1104,A05->1105"));
        convGrp.addItem(new ConfigItem(KEY_CONV_ROW, "-1/2"));
        convGrp.addItem(new ConfigItem(KEY_CONV_COLUMN, "-1"));
        convGrp.addItem(new ConfigItem(KEY_CONV_TIER, "-1"));
        _conversionGrp = convGrp;

        // First convert a TOS pos to its aicon equivalents
        String tosPos = "A024908.2";
        LinkedHashMap<String, String> tosMap = splitPosition(tosPos);
        Map<String, String> aiconMap = convertPosPartsToAicon(tosMap);
        assertNotNull(aiconMap);
        assertEquals(4, aiconMap.size());
        assertEquals("1102", aiconMap.get(POS_PART_BLOCK));
        assertEquals("24", aiconMap.get(POS_PART_ROW));
        assertEquals("7", aiconMap.get(POS_PART_COLUMN));
        assertEquals("1", aiconMap.get(POS_PART_TIER));

        // so and now back to tos parts and merge them together to check them with the original position.
        Map<String, String> tosMap2 = convertPosPartsToTos(aiconMap);
        String tosPos2 = mergePosition(collectParts(
                        tosMap2.get(POS_PART_BLOCK),
                        tosMap2.get(POS_PART_ROW),
                        tosMap2.get(POS_PART_COLUMN),
                        tosMap2.get(POS_PART_TIER)),
                false, false
        );
        // verify that position after conversion to Aicon and back to TOS results in same starting position.
        assertEquals(tosPos, tosPos2);
    }
}
