package com.aicon.tos.connect.cdc;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.web.pages.DataStore;
import com.aicon.tos.shared.AiconTosMediatorConfig;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.aicon.tos.ConfigDomain.getPrefixedTopic;
import static java.lang.Math.abs;

/**
 * Processes CDC (Change Data Capture) data to validate the consistency and integrity between TOS and AICON systems.
 */
public class CDCDataProcessor {

    private static final Logger LOG = LoggerFactory.getLogger(CDCDataProcessor.class);
    private final List<CDCConfig> cdcConfigTable;
    private final DataStore dataStore;

    public CDCDataProcessor(ConfigGroup canaryConfig) {
        this.cdcConfigTable = getCDCConfigTable(canaryConfig);
        dataStore = DataStore.getInstance();
    }

    /**
     * Processes CDC data to compare entries between TOS and AICON systems based on configuration thresholds.
     *
     * @param cdcTOSData   Iterable collection of CDCData from the TOS system.
     * @param cdcAICONData Iterable collection of CDCData from the AICON system.
     * @return Boolean indicating if the CDC data is consistent (true) or not (false).
     */
    public Boolean processCDCData(List<CDCData> cdcTOSData, List<CDCData> cdcAICONData) {
        boolean cdcOk = true;

        if (cdcConfigTable.isEmpty()) {
            return false;
        }

        // Iterate through the TOS data list
        for (CDCData tosData : cdcTOSData) {
            // Search for a match in the AICON data list based on tableName and gkey

            boolean tableAndKeyFound = false;
            CDCData aiconDataMatch = null;

            CDCConfig cdcConfig = getCdcConfigForTable(tosData.getTableName());
            if (cdcConfig == null) {
                LOG.error("CDC table form TOS not available in configuration");
                return false;
            }

            for (CDCData aiconData : cdcAICONData) {
                if (tosData.getTableName().equals(aiconData.getTableName()) &&
                        tosData.getGkey().equals(aiconData.getGkey())) {
                    tableAndKeyFound = true;
                    aiconDataMatch = aiconData;
                    break;
                }
            }

            if (tableAndKeyFound) {    // Look for the correct threshold in the configList based on the tableName
                long threshold = cdcConfig.threshold();

                // Calculate the difference in timestamps
                long timeDifference = abs(
                        tosData.getCreationTimestampAsLong() - aiconDataMatch.getCreationTimestampAsLong()
                                + dataStore.getTimeSync());

                // Check if the time difference is within the threshold
                if (timeDifference <= threshold) {
                    LOG.info("Match found: Table={}, GKey={}, Time difference={}ms (within threshold)",
                            tosData.getTableName(), tosData.getGkey(), timeDifference);
                } else {
                    LOG.info("Mismatch: Table={}, GKey={}, Time difference={}ms (outside threshold)",
                            tosData.getTableName(), tosData.getGkey(), timeDifference);
                    cdcOk = false;
                }
            } else {
                LOG.info("No threshold found for Table={}", tosData.getTableName());
                cdcOk = false;
            }
        }

        return cdcOk;
    }

    // format:   table=key:timestamp;table=key:timestamp;table=key:timestamp
    public List<CDCData> convertStringToCDCData(CharSequence cdcInfoFromTos) {
        List<CDCData> cdcDataList = new ArrayList<>();

        if (null != cdcInfoFromTos) {
            // Convert CharSequence to String and split the table data by commas
            String[] entries = cdcInfoFromTos.toString().split(",");

            // Loop through each table entry
            for (String entry : entries) {
                // Split by '=' to separate the table name and the rest of the data
                String[] tableAndData = entry.split("=");
                if (AiconTosMediatorConfig.CORRECT_LENGTH_AFTER_SPLIT == tableAndData.length) {
                    String tableName = tableAndData[0].trim();  // Table name

                    // Split by ':' to separate the gkey and creationTimestamp
                    String[] gkeyAndTimestamp = tableAndData[1].split(":");
                    if (AiconTosMediatorConfig.CORRECT_LENGTH_AFTER_SPLIT == gkeyAndTimestamp.length) {
                        String gkey = gkeyAndTimestamp[0].trim();  // gkey
                        String creationTimestamp = gkeyAndTimestamp[1].trim();  // creationTimestamp

                        if (!creationTimestamp.equals("null")) {
                            // Create a new CDCData object and add it to the list
                            cdcDataList.add(new CDCData(tableName, gkey, Long.parseLong(creationTimestamp)));
                        }
                    }
                }
            }
        }
        return cdcDataList;
    }

    public String convertCDCDataToString(List<CDCData> cdcData) {
        if (cdcData == null || cdcData.isEmpty()) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        for (CDCData data : cdcData) {
            stringBuilder.append(data.getTableName())
                    .append("=")
                    .append(data.getGkey())
                    .append(":")
                    .append(data.getCreationTimestamp())
                    .append(",");
        }

        if (!stringBuilder.isEmpty()) {
            stringBuilder.deleteCharAt(stringBuilder.length() - 1);
        }

        return stringBuilder.toString();
    }

    public String convertRawStringToPrettyCDCString(String cdcDataStr) {
        List<CDCData> cdcData = convertStringToCDCData(cdcDataStr);
        return cdcData.stream()
                .map(data -> String.format("%s=%s:%s",
                        data.getTableName(),
                        data.getGkey(),
                        longTimestampToString(data.getCreationTimestamp())))
                .collect(Collectors.joining(";\n"));
    }

    public String longTimestampToString(Long timestamp) {
        Instant instant = Instant.ofEpochMilli(timestamp);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault());
        return formatter.format(instant);
    }

    /**
     * Determines the CDC status based on the input CDC table information.
     *
     * @param tosCDCDataStr   The CDC table information from TOS.
     * @param aiconCDCDataStr The CDC table information from AICON.
     * @return Boolean indicating if the CDC status is OK.
     */
    public Boolean determineCdcStatus(String tosCDCDataStr, String aiconCDCDataStr) {
        return determineCdcStatus(tosCDCDataStr, convertStringToCDCData(aiconCDCDataStr));
    }

    /**
     * Determines the CDC status based on the input CDC table information.
     *
     * @param tosCDCDataStr The CDC table information from TOS.
     * @param aiconCDCData  The CDC table information from AICON
     * @return Boolean indicating if the CDC status is OK.
     */
    public Boolean determineCdcStatus(String tosCDCDataStr, List<CDCData> aiconCDCData) {
        Boolean newCdcOk;

        //if AICON data contains a null in creationTimestamp it wasn't found within threshold
        boolean aiconDataOk = aiconCDCData.stream()
                .allMatch(cdcData -> (cdcData.getCreationTimestamp() != null &&
                        freshData(cdcData.getCreationTimestamp())));
        if (!aiconDataOk) {
            String topicsWithNullTimestamp = aiconCDCData.stream()
                    .filter(cdcData -> cdcData.getCreationTimestamp() == null) // Filter items with null timestamp
                    .map(CDCData::getTableName) // Extract topic names
                    .collect(Collectors.joining(", ")); // Join them into a single string
            LOG.info("Missing CDC data for topics {}", topicsWithNullTimestamp);
            return false;
        }

        List<CDCData> tosCDCData = convertStringToCDCData(tosCDCDataStr);
        if (tosCDCData.isEmpty() && aiconCDCData.isEmpty()) { //No CDC tables
            newCdcOk = true;
        } else {
            if ((!tosCDCData.isEmpty() && !aiconCDCData.isEmpty())) {
                newCdcOk = processCDCData(tosCDCData, aiconCDCData);
            } else {
                newCdcOk = false;
            }
        }
        return newCdcOk;
    }

    /**
     * Extracts the `tableName` fields from a list of CDCConfigTable.
     *
     * @return A list of `tableName` values.
     */
    public List<String> getCDCTableNamesList() {
        return cdcConfigTable.stream().map(CDCConfig::tableName).collect(Collectors.toList());
    }

    public String getCDCTableNames() {
        return String.join(",", getCDCTableNamesList());
    }

    public String getSimulatedAiconCDCData(String cdcDataIn, long userSetLag) {
        List<CDCData> cdcData = convertStringToCDCData(cdcDataIn);
        for (CDCData cdc : cdcData) {
            cdc.setCreationTimestamp(cdc.getCreationTimestampAsLong() + userSetLag);
        }
        return convertCDCDataToString(cdcData);
    }

    public boolean expectedTOSCDCDataForConfiguredTables(String tosCDCDataStr) {
        List<CDCData> cdcData = convertStringToCDCData(tosCDCDataStr);
        return expectedTOSCDCDataForConfiguredTables(cdcData);
    }

    public boolean expectedTOSCDCDataForConfiguredTables(List<CDCData> tosCDCData) {
        // Extract the relevant field from tosCDCData (e.g., table names)
        Set<String> tableNames = tosCDCData.stream()
                .map(CDCData::getTableName) // Assuming CDCData has a method getTableName()
                .collect(Collectors.toSet());

        // Check if all keys in thresholdMap are in the extracted table names
        return tableNames.containsAll(getCDCTableNamesList());
    }

    private List<CDCConfig> getCDCConfigTable(ConfigGroup canaryConfig) {
        List<CDCConfig> result = new ArrayList<>();
        List<String> cdcTableNames = canaryConfig.getChildrenWithNames(ConfigType.CDCTable);
        List<ConfigGroup> cdcTableConfigs = canaryConfig.getChildren().stream()
                .filter(configGroup -> cdcTableNames.contains(configGroup.getName()))
                .toList();

        for (ConfigGroup configGroup : cdcTableConfigs) {
            String tableName = configGroup.getName();
            String topicName = getPrefixedTopic(configGroup, ConfigDomain.CFG_CDC_TOPIC_NAME);
            String groupId = configGroup.getItemValue(ConfigDomain.CFG_CDC_GROUP_ID, null);
            Long threshold = Long.valueOf(configGroup.getItemValue(ConfigDomain.CFG_CDC_THRESHOLD, "0"));

            CDCConfig cdcConfig = new CDCConfig(tableName, topicName, groupId, threshold);
            result.add(cdcConfig);
        }
        return result;
    }

    public List<CDCConfig> getCdcConfigTable() {
        return cdcConfigTable; // Collects them into a List
    }

    public CDCConfig getCdcConfigForTable(String tableName) {
        for (CDCConfig cdcConfig : cdcConfigTable) {
            if (cdcConfig.tableName().equals(tableName)) {
                return cdcConfig;
            }
        }
        return null;
    }

    public String removeTooOldData(String cdcDataStr) {
        List<CDCData> cDCData = convertStringToCDCData(cdcDataStr);
        List<CDCData> newCDCData = removeTooOldData(cDCData);
        return convertCDCDataToString(newCDCData);
    }

    public List<CDCData> removeTooOldData(List<CDCData> tosCDCDataTable) {
        List<CDCData> newTosCDCDataTable = new ArrayList<>();
        for (CDCData cdcData : tosCDCDataTable) {
            if (freshData(cdcData.getCreationTimestamp())) {
                newTosCDCDataTable.add(cdcData);
            }
        }
        return newTosCDCDataTable;
    }

    private boolean freshData(Long creationTimestamp) {
        int MAX_DUE_TIME_MSEC = 24 * 60 * 60 * 1000; // max 24 hours
        return creationTimestamp + MAX_DUE_TIME_MSEC >= Instant.now().toEpochMilli();
    }

    public String getCdcConfigTableString() {
        StringBuilder result = new StringBuilder();
        for (CDCConfig cdcConfig : getCdcConfigTable()) {
            if (!result.isEmpty()) {
                result.append("\n");
            }
            result.append(cdcConfig.toString());
        }
        return result.toString();
    }
}
