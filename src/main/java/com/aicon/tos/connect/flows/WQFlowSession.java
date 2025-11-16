package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.http.JsonParser;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.kafka.AiconTosConnectWQResponseTopicProducer;
import com.avlino.aicon.WorkQueueActivationResponse.dispatch_work_queue_activation_response_key;
import com.avlino.aicon.WorkQueueActivationResponse.dispatch_work_queue_activation_response_value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WQFlowSession extends HttpFlowSession {
    protected static final Logger LOG = LoggerFactory.getLogger(WQFlowSession.class);
    private final String json;

    public WQFlowSession(FlowController flowController,
                         String jsonRecord,
                         ConfigGroup kafkaConfig, ConfigGroup flowConfig, ConfigGroup httpConfig,
                         RequestResponseTransformer transformer, String n4Scope) {
        super(flowController, "WQ_FLOW", httpConfig, flowConfig, kafkaConfig, transformer, n4Scope);
        this.json = jsonRecord;
        LOG.info("Instantiate {} FlowSession", flowConfig.getName());
    }

    @Override
    public void execute() throws FlowSessionException {
        LOG.info("Starting WQ FLOW session");

        String path = flowConfig.getItemValue(ConfigDomain.CFG_FLOW_URL_PATH, "");
        try {
            String response = connector.sendHttpRequest(httpConfig, json);
            LOG.info("Raw Response = {}", response);

            // Temporary patch for client bug
            if (response.contains("\"APIIdentifier\":null")) {
                response = response.replace("\"APIIdentifier\":null", "\"APIIdentifier\":14");
            }

            dispatch_work_queue_activation_response_value responseValue =
                    JsonParser.convertJsonToAvro(response, dispatch_work_queue_activation_response_value.class);

            dispatch_work_queue_activation_response_key msgKey = new dispatch_work_queue_activation_response_key();
            msgKey.setRequestIdx(responseValue.getRequestIdx());
            msgKey.setApiIdentifier(responseValue.getAPIIdentifier());
            msgKey.setTimestamp(responseValue.getTimestamp());

            dispatch_work_queue_activation_response_value msgValue = new dispatch_work_queue_activation_response_value();
            msgValue.setRequestIdx(msgKey.getRequestIdx());
            msgValue.setAPIIdentifier(msgKey.getApiIdentifier());
            msgValue.setTimestamp(msgKey.getTimestamp());
            msgValue.setErrMsg(responseValue.getErrMsg());
            msgValue.setErrCode(responseValue.getErrCode());

            AiconTosConnectWQResponseTopicProducer producer = new AiconTosConnectWQResponseTopicProducer();
            producer.sendMessage(msgKey.toString(), msgValue);
        } catch(Exception e) {
            throw new FlowSessionException("WQFlowSession execution failed", e);
        }
    }

    @Override
    protected String getThreadName(int threadCounter) {
        return String.format("%s-%s", flowConfig.getName(), threadCounter);
    }
}
