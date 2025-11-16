package com.aicon.tos.interceptor.decide;

import java.util.List;
import java.util.Objects;

/**
 * Helper class representing the Aicon Decking Request schema.
 */
public class AiconDeckingEngineRequest {

    private String requestIndex = "UNKNOWN";
    private int count = 0;
    private long timeStamp;
    private Integer errorCode; // Nullable
    private String errorDesc; // Nullable
    private List<Request> requests;

    public static class Request {
        private String containerNumber = "UNKNOWN";
        private String containerVisitId = "UNKNOWN";
        private String jobType = "UNKNOWN";
        private String uptUserId = "UNKNOWN";
        private String programId = "UNKNOWN";
        private String blockIndexNumber = "UNKNOWN";
        private String bayIndexNumber = "UNKNOWN";
        private String rowIndexNumber = "UNKNOWN";
        private String tierIndexNumber = "UNKNOWN";
        private String internalIndex1; // Nullable
        private String internalIndex2; // Nullable
        private boolean requestedBlockOnly = false;
        private boolean isEmptyContainer =false;
        private Integer errorCode; // Nullable
        private String errorDesc; // Nullable

        // Getters and setters
        public String getContainerNumber() {
            return containerNumber;
        }

        public void setContainerNumber(String containerNumber) {
            this.containerNumber = containerNumber;
        }

        public String getContainerVisitId() {
            return containerVisitId;
        }

        public void setContainerVisitId(String containerVisitId) {
            this.containerVisitId = containerVisitId;
        }

        public String getJobType() {
            return jobType;
        }

        public void setJobType(String jobType) {
            this.jobType = jobType;
        }

        public String getUptUserId() {
            return uptUserId;
        }

        public void setUptUserId(String uptUserId) {
            this.uptUserId = uptUserId;
        }

        public String getProgramId() {
            return programId;
        }

        public void setProgramId(String programId) {
            this.programId = programId;
        }

        public String getBlockIndexNumber() {
            return blockIndexNumber;
        }

        public void setBlockIndexNumber(String blockIndexNumber) {
            this.blockIndexNumber = blockIndexNumber;
        }

        public String getBayIndexNumber() {
            return bayIndexNumber;
        }

        public void setBayIndexNumber(String bayIndexNumber) {
            this.bayIndexNumber = bayIndexNumber;
        }

        public String getRowIndexNumber() {
            return rowIndexNumber;
        }

        public void setRowIndexNumber(String rowIndexNumber) {
            this.rowIndexNumber = rowIndexNumber;
        }

        public String getTierIndexNumber() {
            return tierIndexNumber;
        }

        public void setTierIndexNumber(String tierIndexNumber) {
            this.tierIndexNumber = tierIndexNumber;
        }

        public String getInternalIndex1() {
            return internalIndex1;
        }

        public void setInternalIndex1(String internalIndex1) {
            this.internalIndex1 = internalIndex1;
        }

        public String getInternalIndex2() {
            return internalIndex2;
        }

        public void setInternalIndex2(String internalIndex2) {
            this.internalIndex2 = internalIndex2;
        }

        public Integer getErrorCode() {
            return errorCode;
        }

        public void setErrorCode(Integer errorCode) {
            this.errorCode = errorCode;
        }

        public String getErrorDesc() {
            return errorDesc;
        }

        public void setErrorDesc(String errorDesc) {
            this.errorDesc = errorDesc;
        }

        public boolean isrequestedBlockOnly() {
            return requestedBlockOnly;
        }

        public void setrequestedBlockOnly(boolean requestedBlockOnly) {
            this.requestedBlockOnly = requestedBlockOnly;
        }

        public boolean isEmptyContainer() {
            return isEmptyContainer;
        }

        public void setEmptyContainer(boolean emptyContainer) {
            isEmptyContainer = emptyContainer;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Request request = (Request) o;
            return Objects.equals(containerNumber, request.containerNumber) &&
                    Objects.equals(containerVisitId, request.containerVisitId) &&
                    Objects.equals(jobType, request.jobType) &&
                    Objects.equals(uptUserId, request.uptUserId) &&
                    Objects.equals(programId, request.programId) &&
                    Objects.equals(blockIndexNumber, request.blockIndexNumber) &&
                    Objects.equals(bayIndexNumber, request.bayIndexNumber) &&
                    Objects.equals(rowIndexNumber, request.rowIndexNumber) &&
                    Objects.equals(tierIndexNumber, request.tierIndexNumber) &&
                    Objects.equals(internalIndex1, request.internalIndex1) &&
                    Objects.equals(internalIndex2, request.internalIndex2) &&
                    Objects.equals(requestedBlockOnly, request.requestedBlockOnly) &&
                    Objects.equals(isEmptyContainer, request.isEmptyContainer) &&
                    Objects.equals(errorCode, request.errorCode) &&
                    Objects.equals(errorDesc, request.errorDesc);
        }

        @Override
        public int hashCode() {
            return Objects.hash(containerNumber, containerVisitId, jobType, uptUserId, programId,
                    blockIndexNumber, bayIndexNumber, rowIndexNumber, tierIndexNumber,
                    internalIndex1, internalIndex2, errorCode, errorDesc);
        }
    }

    // Getters and setters
    public String getRequestIndex() {
        return requestIndex;
    }

    public void setRequestIndex(String requestIndex) {
        this.requestIndex = requestIndex;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public Integer getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(Integer errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorDesc() {
        return errorDesc;
    }

    public void setErrorDesc(String errorDesc) {
        this.errorDesc = errorDesc;
    }

    public List<Request> getRequests() {
        return requests;
    }

    public void setRequests(List<Request> requests) {
        this.requests = requests;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AiconDeckingEngineRequest that = (AiconDeckingEngineRequest) o;
        return count == that.count &&
                timeStamp == that.timeStamp &&
                Objects.equals(requestIndex, that.requestIndex) &&
                Objects.equals(errorCode, that.errorCode) &&
                Objects.equals(errorDesc, that.errorDesc) &&
                Objects.equals(requests, that.requests);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requestIndex, count, timeStamp, errorCode, errorDesc, requests);
    }
}