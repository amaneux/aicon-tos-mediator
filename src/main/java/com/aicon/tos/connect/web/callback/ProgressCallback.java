package com.aicon.tos.connect.web.callback;

import com.aicon.tos.shared.ResultLevel;

import java.util.Map;

public interface ProgressCallback {

    /**
     * Reports progress to the caller
     *
     * @param state the state of the processing.
     * @param report free text to explain the current state to the caller
     * @param reportMap additional map of reported objects known to both sides (can be null).
     * @return the report again to allow concatenation for Logging etc.
     */
    String reportProgress(
            ResultLevel state,
            String report,
            Map<String, Object> reportMap
    );

    /**
     * Reports Done to the caller.
     * @param state the end state of the processing.
     * @param report free text to explain the end-state to the caller
     * @return the report again to allow concatenation for Logging etc.
     */
    String onDone(
            ResultLevel state,
            String report
    );
}
