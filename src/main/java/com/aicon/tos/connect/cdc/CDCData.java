package com.aicon.tos.connect.cdc;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;


public class CDCData {
    private String tableName;
    private String gkey;
    private Long creationTimestamp; //Millis
    private static final Random random = new Random();

    public CDCData() {
    }

    public CDCData(String tableName, String gkey, Long creationTimestamp) {
        this.tableName = tableName;
        this.gkey = gkey;
        this.creationTimestamp = creationTimestamp;
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getGkey() {
        return gkey;
    }

    public void setGkey(String gkey) {
        this.gkey = gkey;
    }

    public Long getCreationTimestamp() {
        return creationTimestamp;
    }

    @JsonIgnore
    public long getCreationTimestampAsLong() {
        return creationTimestamp;
    }

    public void setCreationTimestamp(Long creationTimestamp) {
        this.creationTimestamp = creationTimestamp;
    }

    public static CDCData createRandomValues(String table) {
        CDCData cdcData = new CDCData();
        cdcData.setTableName(table);
        cdcData.setGkey(String.valueOf(0b101111011L * random.nextInt(1000)));
        cdcData.setCreationTimestamp(generateRandomTimestamp());
        return cdcData;
    }

    public static Long generateRandomTimestamp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime fiveDaysAgo = now.minusDays(5);

        long startEpochMilli = fiveDaysAgo.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endEpochMilli = now.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();

        return ThreadLocalRandom.current().nextLong(startEpochMilli, endEpochMilli);
    }

    public String toXMLString() {
        return this.tableName + "=" + this.gkey + ":" + getCreationTimestampAsLong();
    }

    @Override
    public String toString() {
        return "CDCData{" +
                "tableName='" + tableName + '\'' +
                ", gkey='" + gkey + '\'' +
                ", creationTimestamp='" + creationTimestamp + '\'' +
                '}';
    }
}
