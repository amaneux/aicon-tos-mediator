package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.http.JsonParser;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformerFactory;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.aicon.tos.shared.exceptions.TransformerCreationException;
import com.aicon.tos.shared.kafka.AiconTosConnectSwapRequestTopicConsumer;
import com.aicon.tos.shared.kafka.AiconTosConnectWQRequestTopicConsumer;
import com.aicon.tos.shared.kafka.AiconTosControlConsumer;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestConsumer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.aicon.tos.shared.schema.AiconTosControlMessage;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import com.avlino.aicon.ITVJobResequenceRequest.itv_job_resequence_request_value;
import com.avlino.aicon.WorkQueueActivationRequest.dispatch_work_queue_activation_value;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static com.aicon.tos.connect.flows.BaseController.splitReportLinesIfTooLong;

/**
 * Controls and monitors all steps for a message flow from source to destination, and when it is a synchronous call
 * also the route back to 1 or more (ok/error) response topics.
 * The FlowController subscribes to a single topic in its own thread and spawns a FlowSession object in its own thread
 * to handle the steps to be taken from source until destination and back when relevant.
 * todo yusuf add flows for the aicon-yard-move-request / aicon-dispatch-update-request (so we can take this out of the http-sink-connector)
 * todo the fact that I see hardcoded names here is a bit weird, I think the controller should all be driven by configuration (similar as what we've done for the Scenario's), so no hardcoded names should be seen here, the controller should take care of the plumbing around it.
 */
public class FlowController extends  AbstractSessionController implements Runnable, StoppableController {
    private static final Logger LOG = LoggerFactory.getLogger(FlowController.class);

    Map<FlowSession.SessionState, Long> stats;
    private final Map<FlowSession, Thread> sessions;
    private boolean running = true;

    AiconTosConnectSwapRequestTopicConsumer aiconTosConnectSwapRequestTopicConsumer;
    AiconTosConnectWQRequestTopicConsumer aiconTosConnectWQRequestTopicConsumer;
    AiconTosControlConsumer aiconTosControlConsumer;
    AiconYardDeckingUpdateRequestConsumer aiconYardDeckingUpdateRequestMessageConsumer;
    private RequestResponseTransformer transformer;
    FlowSession.SessionState sessionState = null;
    String sessionStateMsg = null;
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");

    ConfigGroup flowConfig;
    private String controlFlowGroupId = null;

    private int threadCounter = 0;

    public FlowController(FlowManager flowManager, ConfigGroup flowConfig) {
        super(flowManager);
        
        this.flowConfig = flowConfig;

        String kafkaRef = (String) this.flowConfig.getChildGroup(ConfigType.KafkaRef).getRef();
        this.kafkaConfig = getConnectionsConfig().getReferencedGroup(kafkaRef, ConfigType.Kafka);
        String httpRef = (String) this.flowConfig.getChildGroup(ConfigType.HttpRef).getRef();
        this.httpConfig = getConnectionsConfig().getReferencedGroup(httpRef, ConfigType.Http);

        LOG.info("Instantiate FLOW Controller for {}", flowConfig.getName());

        if (flowConfig.getName().equals("ControlFlow")) {
            // Use different kafka group for this flow
            ConfigSettings configSettings = ConfigSettings.reloadConfigFromFile(); //TODO To flowmanager
            ConfigGroup controlRequestConfig = configSettings.getMainGroup(ConfigType.TosControl).getChildGroup(ConfigType.ControlRequest);
            controlFlowGroupId = controlRequestConfig.getItemValue(ConfigSettings.CFG_KAFKA_GROUP_ID);
        }

        String transformerClass = httpConfig.getItemValue(ConfigSettings.CFG_HTTP_TRANSFORMER_CLASS);
        if (transformerClass == null) {
            transformer = null;
        } else {
            try {
                transformer = RequestResponseTransformerFactory.getTransformer(transformerClass);
            } catch (TransformerCreationException e) {
                LOG.error("Failed to create transformer: {}", e.getMessage(), e);
                // Handle the failure (e.g., fallback to default logic)
            }
        }

        this.sessions = new LinkedHashMap<>();
        stats = new LinkedHashMap<>(FlowSession.SessionState.values().length);
        for (FlowSession.SessionState state : FlowSession.SessionState.values()) {
            stats.put(state, 0L);
        }
    }

    @Override
    public void run() {

        try {
            String sourceTopic = flowConfig.getItemValue(ConfigDomain.CFG_FLOW_TOPIC_SOURCE);
            LOG.info("Starting FlowController {} listening on topic {}", flowConfig.getName(), sourceTopic);

            if (Objects.equals(sourceTopic, KafkaConfig.aicon_dispatch_itv_job_resequence_request_topic)) {
                while (running) {
                    procesItvTopic();
                }
            }
            if (Objects.equals(sourceTopic, KafkaConfig.aicon_dispatch_work_queue_activation_request_topic)) {
                while (running) {
                    processWqTopic();
                }
            }
            if (Objects.equals(sourceTopic, KafkaConfig.AICON_TOS_CONTROL_TOPIC)) {
                while (running) {
                    processAiconTosControlTopic();
                }
            }
            if (Objects.equals(sourceTopic, KafkaConfig.getFlowRequestTopic("DeckingUpdateFlow"))) {
                while (running) {
                    processTosDeckingUpdateTopic();
                }
            }
            LOG.info("FlowController {} finished run action", flowConfig.getName());
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            throw new RuntimeException(e);
        } finally {
            if (aiconTosConnectSwapRequestTopicConsumer != null) {
                aiconTosConnectSwapRequestTopicConsumer.stop();
            }
            if (aiconTosConnectWQRequestTopicConsumer != null) {
                aiconTosConnectWQRequestTopicConsumer.stop();
            }
            if (aiconTosControlConsumer != null) {
                aiconTosControlConsumer.stop();
            }
            if (aiconYardDeckingUpdateRequestMessageConsumer != null) {
                aiconYardDeckingUpdateRequestMessageConsumer.stop();
            }
        }
    }

    private void procesItvTopic() {
        aiconTosConnectSwapRequestTopicConsumer = new AiconTosConnectSwapRequestTopicConsumer();
        while (running) {
            ConsumerRecords<String, itv_job_resequence_request_value> records =
                    aiconTosConnectSwapRequestTopicConsumer.pollMessages();

            if (records.count() > 0) {
                for (ConsumerRecord<String, itv_job_resequence_request_value> consumerRecord : records) {
                    String json = JsonParser.convertAvroToJson(consumerRecord.value());

                    FlowSession flowSession = new SwapFlowSession(
                            this, json, getKafkaConfig(), flowConfig, getHttpConfig(), transformer, getN4Scope());
                    Thread thread = new Thread(flowSession, flowSession.getThreadName((threadCounter++)));
                    sessions.put(flowSession, thread);
                    thread.start();
                }
            }
        }
        aiconTosConnectSwapRequestTopicConsumer.stop();
    }

    /**
     * Processes messages from the AICON TOS Work Queue request Kafka topic and initiates
     * flow sessions for each received message. The method continuously polls the Kafka topic
     * for messages while the application is in a running state and handles each message
     * on a separate thread.
     * <p>
     * Functionality:
     * - Initializes an instance of {@link AiconTosConnectWQRequestTopicConsumer} for consuming messages.
     * - Polls the Kafka topic to retrieve batches of messages.
     * - For each received message:
     * - Converts the message value from Avro format to JSON using {@link JsonParser}.
     * - Creates a {@link WQFlowSession} to handle the message processing with the specified configurations.
     * - Starts a new thread for the flow session and maps the flow session to its thread in the 'sessions' map.
     * - Continuously processes messages in a loop until the 'running' flag is set to false.
     * - Stops the Kafka consumer when the application is no longer running.
     * <p>
     * Threading:
     * - Each flow session executes on its own thread, allowing concurrent processing of multiple messages.
     * - Tracks and manages threads via the 'sessions' map to ensure proper organization and potential cleanup.
     * <p>
     * Important Notes:
     * - Relies on synchronized management of shared resources to ensure thread safety.
     * - Assumes external control of the 'running' flag to terminate the infinite polling loop gracefully.
     */
    private void processWqTopic() {
        aiconTosConnectWQRequestTopicConsumer = new AiconTosConnectWQRequestTopicConsumer();
        while (running) {
            ConsumerRecords<String, dispatch_work_queue_activation_value> records =
                    aiconTosConnectWQRequestTopicConsumer.pollMessages();

            if (records.count() > 0) {
                for (ConsumerRecord<String, dispatch_work_queue_activation_value> consumerRecord : records) {
                    String json = JsonParser.convertAvroToJson(consumerRecord.value());
                    FlowSession flowSession = new WQFlowSession(
                            this, json, getKafkaConfig(), flowConfig, flowConfig ,transformer, getN4Scope());
                    Thread thread = new Thread(flowSession, flowSession.getThreadName(threadCounter++));
                    sessions.put(flowSession, thread);
                    thread.start();
                }
            }
        }
        aiconTosConnectWQRequestTopicConsumer.stop();
    }

    /**
     * This method processes messages from the AICON TOS Control Kafka topic and initiates
     * flow sessions for each received message. It continuously polls for messages while
     * the application is in a running state and creates threads for handling each message.
     * When the application stops running, the associated consumer is also stopped.
     * <p>
     * Functionality:
     * - Initializes an instance of {@link AiconTosControlConsumer} using the specified control flow group ID.
     * - Continuously checks for new Kafka messages while the application is running.
     * - For each message, creates a {@link ControlFlowSession} to handle the message processing.
     * - Starts a new thread for each flow session and maintains a mapping of flow sessions to threads in the 'sessions' map.
     * - Stops the AICON TOS Control Consumer when the running flag is set to false.
     * <p>
     * Threading:
     * - Each flow session runs on its own thread, enabling concurrent processing of multiple messages.
     * - Threads for flow sessions are managed in the 'sessions' map.
     * <p>
     * Important Notes:
     * - Uses synchronized data structures and multithreading, requiring careful management of shared resources.
     * - Method assumes the running flag is externally managed and set to false to terminate the infinite polling loop.
     */
    private void processAiconTosControlTopic() {

        aiconTosControlConsumer = new AiconTosControlConsumer(controlFlowGroupId);
        while (running) {
            ConsumerRecords<String, AiconTosControlMessage> records = aiconTosControlConsumer.pollMessages();

            if (records.count() > 0) {
                for (ConsumerRecord<String, AiconTosControlMessage> consumerRecord : records) {
                    FlowSession flowSession = new ControlFlowSession(this, consumerRecord, getKafkaConfig(),
                            flowConfig, getHttpConfig(), transformer, getN4Scope());
                    Thread thread = new Thread(flowSession, flowSession.getThreadName(threadCounter++));
                    sessions.put(flowSession, thread);
                    thread.start();
                }
            }
        }
        aiconTosControlConsumer.stop();
    }

    private void processTosDeckingUpdateTopic() {

        aiconYardDeckingUpdateRequestMessageConsumer = new AiconYardDeckingUpdateRequestConsumer();
        while (running) {
//            LOG.info("DeckingUpdate flow is running and running = {} ", running);
            ConsumerRecords<String, AiconYardDeckingUpdateRequestMessage> records = aiconYardDeckingUpdateRequestMessageConsumer.pollMessages();

            if (records.count() > 0) {
                for (ConsumerRecord<String, AiconYardDeckingUpdateRequestMessage> consumerRecord : records) {
                    FlowSession flowSession = new DeckingUpdateFlowSession(this, consumerRecord,
                            flowConfig, getHttpConfig(), transformer, getN4Scope());
                    Thread thread = new Thread(flowSession, flowSession.getThreadName(threadCounter++));
                    sessions.put(flowSession, thread);
                    thread.start();
                }
            }
        }
        aiconYardDeckingUpdateRequestMessageConsumer.stop();
    }

    public void stopController() {
        LOG.info("Stopping FlowController for {}", flowConfig.getName());
        running = false;

        Thread.currentThread().interrupt();
    }

    public boolean isRunning() {
        return running;
    }

    public String toString() {
        return String.format("FlowController: %s", flowConfig);
    }

    private static final String REPORT_TEMPLATE =
            "FlowController %s subscribed to %s, threadState=%s, endPoint=%s, %s = %s\n\tSessionsStats: #ACTIVE=%s, DONE=%s, FAILED=%s \n\t\t%s";
    private static final String NO_ENDPOINT_FOUND_MESSAGE = "No Endpoint connector found (bad reference)";

    /**
     * Generates a detailed report of the current state and configuration
     * of the flow controller, including information such as the endpoint,
     * thread state, flow name, topic source, session statistics, and
     * the last session message. The report is formatted using a predefined
     * template and includes newlines and indentation for readability.
     *
     * @return a formatted report string containing the state and configuration
     * details of the flow controller
     */
    public String report() {
        String endpointInfo = getEndpointInfo();
        String threadState = Thread.currentThread().getState().toString();
        String flowName = flowConfig.getName();
        String flowTopicSource = flowConfig.getItemValue(ConfigDomain.CFG_FLOW_TOPIC_SOURCE);
        int activeSessions = sessions.size();
        String doneSessions = stats.get(FlowSession.SessionState.DONE).toString();
        String failedSessions = stats.get(FlowSession.SessionState.FAILED).toString();
        String lastSessionMessage = getLastSessionMsg();
        String groupIdName = ConfigSettings.CFG_KAFKA_GROUP_ID;
        String groupIdValue = "";


        if (flowConfig.getName().equals("ControlFlow")) {
            groupIdValue = controlFlowGroupId;
        } else {
            groupIdValue = this.kafkaConfig.getItemValue(ConfigSettings.CFG_KAFKA_GROUP_ID);
        }

        return splitReportLinesIfTooLong(String.format(REPORT_TEMPLATE,
                flowName, flowTopicSource, threadState, endpointInfo, groupIdName, groupIdValue,
                activeSessions, doneSessions, failedSessions, lastSessionMessage)
        );
    }

    private String getEndpointInfo() {
        if (getHttpConfig() == null) {
            return NO_ENDPOINT_FOUND_MESSAGE;
        }
        return String.format("%s:%s", getHttpConfig().getType(), getHttpConfig().getName());
    }

    public synchronized void sessionEnded(FlowSession flowSession) {
        LOG.info("{} Ended.", flowSession);
        stats.put(flowSession.getState(), stats.get(flowSession.getState()) + 1);
        sessions.remove(flowSession);
    }

    public void setSessionState(FlowSession.SessionState state, String msg) {
        this.sessionState = state;
        this.sessionStateMsg = msg;
    }

    public String getLastSessionMsg() {
        if (sessionState == null) {
            return "No sessions yet.";
        } else {
            String msg = sessionStateMsg == null || sessionStateMsg.isEmpty() ? "" :
                    ", error=" + sessionStateMsg;
            return String.format("Last session finished @ %s, State=%s %s", sdf.format(new Date()),
                    sessionState, msg);
        }
    }
}
