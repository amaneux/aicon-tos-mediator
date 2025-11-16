package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.http.JsonParser;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.kafka.AiconTosConnectSwapResponseTopicProducer;
import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_key;
import com.avlino.aicon.ITVJobResequenceResponse.itv_job_resequence_response_value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A FlowSession takes care of all steps to be taken for the received message to get it to the configured endpoint and
 * handle any response.
 */
public class SwapFlowSession extends HttpFlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(SwapFlowSession.class);
    private final String json;

    public SwapFlowSession(FlowController flowController,
                           String jsonRecord,
                           ConfigGroup kafkaConfig, ConfigGroup flowConfig, ConfigGroup httpConfig,
                           RequestResponseTransformer transformer, String n4Scope) {
        super(flowController, "SWAP_FLOW", httpConfig, flowConfig, kafkaConfig, transformer, n4Scope);
        this.json = jsonRecord;
        LOG.info("Instantiate Swap FLOW session");
    }

    @Override
    public void execute() throws FlowSessionException {

        LOG.info("Starting Swap FLOW session");

        String path = flowConfig.getItemValue(ConfigDomain.CFG_FLOW_URL_PATH, "");
        try {
            String rawResponse = connector.sendHttpRequest(httpConfig, json);

            LOG.info("Raw Response = {}", rawResponse);

            itv_job_resequence_response_value responseValue =
                    JsonParser.convertJsonToAvro(rawResponse, itv_job_resequence_response_value.class);

            logConvertedRecord(responseValue);

            AiconTosConnectSwapResponseTopicProducer producer = new AiconTosConnectSwapResponseTopicProducer();

            itv_job_resequence_response_key msgKey = new itv_job_resequence_response_key();
            msgKey.setRequestIdx(responseValue.getRequestIdx());
            msgKey.setApiIdentifier(responseValue.getAPIIdentifier());
            msgKey.setTimestamp(responseValue.getTimestamp());

            itv_job_resequence_response_value msgValue = new itv_job_resequence_response_value();
            msgValue.setRequestIdx(msgKey.getRequestIdx());
            msgValue.setAPIIdentifier(msgKey.getApiIdentifier());
            msgValue.setTimestamp(msgKey.getTimestamp());
            msgValue.setErrMsg(responseValue.getErrMsg());
            msgValue.setErrCode(responseValue.getErrCode());

            producer.sendMessage(msgKey.toString(), msgValue);
        } catch (Exception e) {
            throw new FlowSessionException("SwapFlowSession execution failed", e);
        }
    }

    private void logConvertedRecord(itv_job_resequence_response_value responseValue) {
        if (LOG.isInfoEnabled()) {
            LOG.info(formatLogMessage(responseValue));
        }
    }

    private String formatLogMessage(itv_job_resequence_response_value responseValue) {
        return String.format(
                "APIIdentifier: %s, requestIdx: %s, timestamp: %s, errCode: %d, errMsg: %s",
                responseValue.getAPIIdentifier(),
                responseValue.getRequestIdx(),
                responseValue.getTimestamp(),
                responseValue.getErrCode(),
                responseValue.getErrMsg()
        );
    }

    @Override
    protected String getThreadName(int threadCounter) {
        return String.format("%s-%s", flowConfig.getName(), threadCounter);
    }
}
