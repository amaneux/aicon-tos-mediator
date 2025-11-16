package com.aicon.tos.connect.web.pages;

import com.aicon.tos.interceptor.newgenproducerconsumer.mock.MockInterceptorConsumer;

import java.util.ArrayList;
import java.util.List;

public class DataStore {
    private static final DataStore instance = new DataStore();
    private String requestDisplayValue = "";
    private String responseEditorValue = "";
    private boolean freezeResponse = false;
    private boolean useModifiedResponse = false;
    private boolean blockResponseSend = false;
    private int sessionsRecordedSize = 0;
    private long cdcLag;
    private long timeSync = 0L;
    private int viewportWidth = 0;
    private List<MockInterceptorConsumer> mockInterceptorConsumers = new ArrayList<>();

    private DataStore() {
    }

    public static DataStore getInstance() {
        return instance;
    }

    // Getters and Setters
    public String getRequestDisplayValue() {
        return requestDisplayValue;
    }

    public void setRequestDisplayValue(String requestDisplayValue) {
        this.requestDisplayValue = requestDisplayValue;
    }

    public String getResponseEditorValue() {
        return responseEditorValue;
    }

    public void setResponseEditorValue(String responseEditorValue) {
        this.responseEditorValue = responseEditorValue;
    }

    public boolean isFreezeResponse() {
        return freezeResponse;
    }

    public void setFreezeResponse(boolean freezeResponse) {
        this.freezeResponse = freezeResponse;
    }

    public boolean isUseModifiedResponse() {
        return useModifiedResponse;
    }

    public void setUseModifiedResponse(boolean useModifiedResponse) {
        this.useModifiedResponse = useModifiedResponse;
    }

    public boolean isBlockResponseSend() {
        return blockResponseSend;
    }

    public void setBlockResponseSend(boolean blockResponseSend) {
        this.blockResponseSend = blockResponseSend;
    }

    public int getSessionsRecordedSize() {
        return sessionsRecordedSize;
    }

    public void setSessionsRecordedSize(int sessionsRecordedSize) {
        this.sessionsRecordedSize = sessionsRecordedSize;
    }

    public long getCDCLag() {
        return cdcLag;
    }

    public void setCDCLag(long cdcLag) {
        this.cdcLag = cdcLag;
    }

    public long getTimeSync() {
        return timeSync;
    }

    public void setTimeSync(long timeSync) {
        this.timeSync = timeSync;
    }

    public void setViewportWidth(int i) {
        if (i == 0) {
            this.viewportWidth = 100;
        }
        this.viewportWidth = i / 8 - 40; //Note width in characters
    }

    public int getViewportWidth() {
        return viewportWidth;
    }

    public void addMockedConsumer(MockInterceptorConsumer mockInterceptorConsumer) {
        mockInterceptorConsumers.add(mockInterceptorConsumer);
    }
}
