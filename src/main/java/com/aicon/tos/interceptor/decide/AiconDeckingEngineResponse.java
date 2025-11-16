package com.aicon.tos.interceptor.decide;

import com.aicon.tos.interceptor.newgenproducerconsumer.messages.AiconMessage;
import com.aicon.tos.shared.ResultEntry;
import org.apache.avro.generic.GenericRecord;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.aicon.tos.shared.ResultLevel.ERROR;
import static com.aicon.tos.shared.ResultLevel.OK;

/**
 * Helper class representing the Aicon Decking Response schema.
 */
public class AiconDeckingEngineResponse {

    private static final List<Integer> OK_ERROR_CODES = List.of(AiconMessage.ERROR_CODE_OK);    // add for testing, 300);

    private String requestIndex;
    private int count;
    private long timeStamp;
    private Integer errorCode; // Nullable
    private String errorDesc; // Nullable
    private ResultEntry result = null;
    private List<Request> requests;

    public static class Request {
        private String containerNumber;
        private String containerVisitId;
        private String jobType;
        private String uptUserId;
        private String programId;
        private String blockIndexNumber;
        private String bayIndexNumber;
        private String rowIndexNumber;
        private String tierIndexNumber;
        private String internalIndex1; // Nullable
        private String internalIndex2; // Nullable
        private boolean requestedBlockOnly = false;
        private boolean isEmptyContainer = false;
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

        public boolean isEmptyContainer() {
            return isEmptyContainer;
        }

        public void setEmptyContainer(boolean emptyContainer) {
            isEmptyContainer = emptyContainer;
        }

        public boolean isrequestedBlockOnly() {
            return requestedBlockOnly;
        }

        public void setrequestedBlockOnly(boolean requestedBlockOnly) {
            this.requestedBlockOnly = requestedBlockOnly;
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
        AiconDeckingEngineResponse that = (AiconDeckingEngineResponse) o;
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

    /**
     * Factory method to convert a GenericRecord into an AiconDeckingEngineResponse.
     */
    public static AiconDeckingEngineResponse fromGenericRecord(GenericRecord genericRecord) {
        AiconDeckingEngineResponse response = new AiconDeckingEngineResponse();

        response.result = new ResultEntry(OK);
        // Map top-level fields
        response.setRequestIndex(genericRecord.get("requestIndex").toString());

        response.setCount((Integer) genericRecord.get("count"));
        response.setTimeStamp((Long) genericRecord.get("timeStamp"));
        response.setErrorCode((Integer) genericRecord.get("errorCode"));
        response.setErrorDesc(getStringValue(genericRecord.get("errorDesc")));

        response.setResult(response.errorCode, response.errorDesc);

        // Map nested "requests" list
        List<GenericRecord> genericRequests = (List<GenericRecord>) genericRecord.get("requests");
        if (genericRequests != null) {
            List<Request> requests = new ArrayList<>();
            for (GenericRecord requestRecord : genericRequests) {
                Request request = new Request();

                request.setContainerNumber(getStringValue(requestRecord.get("containerNumber")));
                request.setContainerVisitId(getStringValue(requestRecord.get("containerVisitId")));
                request.setJobType(getStringValue(requestRecord.get("jobType")));
                request.setUptUserId(getStringValue(requestRecord.get("uptUserId")));
                request.setProgramId(getStringValue(requestRecord.get("programId")));
                request.setBlockIndexNumber(getStringValue(requestRecord.get("blockIndexNumber")));
                request.setBayIndexNumber(getStringValue(requestRecord.get("bayIndexNumber")));
                request.setRowIndexNumber(getStringValue(requestRecord.get("rowIndexNumber")));
                request.setTierIndexNumber(getStringValue(requestRecord.get("tierIndexNumber")));
                request.setInternalIndex1(getStringValue(requestRecord.get("internalIndex1")));
                request.setInternalIndex2(getStringValue(requestRecord.get("internalIndex2")));
                if (requestRecord.get("requestedBlockOnly")!=null) {
                    request.setrequestedBlockOnly((Boolean) requestRecord.get("requestedBlockOnly"));
                }
                if (requestRecord.get("isEmptyContainer")!=null){
                    request.setEmptyContainer((Boolean) requestRecord.get("isEmptyContainer"));
                }
                request.setErrorCode((Integer) requestRecord.get("errorCode"));
                request.setErrorDesc(getStringValue(requestRecord.get("errorDesc")));

                response.setResult(request.errorCode, String.format("%s (%s)", request.errorDesc, request.containerNumber));

                requests.add(request);
            }
            response.setRequests(requests);
        }

        return response;
    }

    private void setResult(Integer errorCode, String errorDesc) {
        if (errorCode != null && !OK_ERROR_CODES.contains(errorCode)) {
            result.overrideWhenNOK(ERROR, errorDesc).setCode(errorCode);
        }
    }

    public ResultEntry getResult() {
        return result;
    }

    private static String getStringValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }
}