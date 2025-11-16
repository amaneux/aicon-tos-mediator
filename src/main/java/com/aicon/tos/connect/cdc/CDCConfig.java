package com.aicon.tos.connect.cdc;

public record CDCConfig(String tableName, String topicName, String groupId, Long threshold) {

    @Override
    public String toString() {
        return "CDCConfig{" +
                "tableName='" + tableName + '\'' +
                ", topicName='" + topicName + '\'' +
                ", groupId='" + groupId + '\'' +
                ", threshold=" + threshold +
                '}';
    }
}
