package com.aicon.tos.connect.flows;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.cdc.CDCData;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformer;
import com.aicon.tos.connect.http.transformers.RequestResponseTransformerFactory;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.aicon.tos.shared.exceptions.TransformerCreationException;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.aicon.tos.connect.flows.BaseController.splitReportLinesIfTooLong;
import static com.aicon.tos.shared.config.ConfigUtil.getConfigItem;
import static com.aicon.tos.shared.util.TimeUtils.waitMilliSeconds;

/**
 * The CanaryController class manages and monitors the execution of canary sessions, ensuring
 * constant checks and reporting for the associated system. It implements the Runnable interface
 * to run as a separate thread and continuously checks system behavior based on the provided
 * configuration and data processors.
 * <p>
 * The CanaryController is responsible for:
 * - Initializing and managing canary session threads.
 * - Coordinating CDC data processing.
 * - Reporting on session states and overall activity.
 * - Storing and retrieving configuration and session statistics.
 * <p>
 * This setup ensures efficient monitoring and supervises the system's behavior over time through
 * a configurable and extendable architecture.
 */
public class CanaryController extends AbstractSessionController implements Runnable, StoppableController {
    private static final Logger LOG = LoggerFactory.getLogger(CanaryController.class);

    private static final int CANARY_INTERVAL_MS = 5000; // Some default if not defined
    private static final int CANARY_CDC_FREQUENCY = 5; // Some default if not defined

    private int canaryIntervalMs = CANARY_INTERVAL_MS;
    private int canaryCDCFrequency = CANARY_CDC_FREQUENCY;
    private int threadCounter = 0;

    private final Map<FlowSession.SessionState, Long> stats;
    private final Map<FlowSession, Thread> sessions;

    private CDCDataProcessor cdcProcessor;
    private final FlowSessionManager sessionHistoryManager;
    private RequestResponseTransformer transformer;
    private final AiconTosConnectionStatusProducer aiconTosConnectionStatusProducer;

    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");

    private String latestTosCDCData = null;
    private String latestAiconCDCData = null;

    private boolean running = true;
    private int cdcCheckCounter = 0;
    private Boolean cdcOk = false;


    public CanaryController(FlowManager flowManager) {

        super(flowManager);

        LOG.info("Instantiate Canary Controller for {}", getCanaryCheckConfig().getName());

        getCanaryConfiguration();

        aiconTosConnectionStatusProducer = new AiconTosConnectionStatusProducer();
        this.sessions = new LinkedHashMap<>();
        stats = new LinkedHashMap<>(FlowSession.SessionState.values().length);
        for (FlowSession.SessionState state : FlowSession.SessionState.values()) {
            stats.put(state, 0L);
        }
        sessionHistoryManager = new FlowSessionManager(30);
    }

    /**
     * Configures and initializes various components required for the CanaryController.
     * <p>
     * This method utilizes the configuration settings from a singleton instance of
     * ConfigSettings to retrieve and set up the necessary configuration groups and
     * components, such as the `canaryCheckConfig`, `httpConfig`, and the `cdcProcessor`.
     * <p>
     * Specifically, the method:
     * - Retrieves the main configuration group for TosControl.
     * - Initializes the CanaryCheck configuration group.
     * - Retrieves a referenced HTTP configuration group based on the canary check's HTTP reference item.
     * - Initializes the CDCDataProcessor instance using the CanaryCheck configuration.
     * - Retrieves additional configuration values as needed.
     * <p>
     * This setup ensures that the CanaryController has access to the required configurations
     * and can perform operations related to canary checks, HTTP interactions, and CDC data processing.
     */
    private void getCanaryConfiguration() {

        String kafkaRef = (String) getCanaryCheckConfig().getChildGroup(ConfigType.KafkaRef).getRef();
        this.kafkaConfig = getConnectionsConfig().getReferencedGroup(kafkaRef, ConfigType.Kafka);
        String httpRef = (String) getCanaryCheckConfig().getChildGroup(ConfigType.HttpRef).getRef();
        this.httpConfig = getConnectionsConfig().getReferencedGroup(httpRef, ConfigType.Http);

        cdcProcessor = new CDCDataProcessor(getCanaryCheckConfig());
        canaryIntervalMs = getConfigItem(getCanaryCheckConfig(), ConfigDomain.CFG_CANARY_INTERVAL_MS, Integer.class, CANARY_INTERVAL_MS);
        canaryCDCFrequency = getConfigItem(getCanaryCheckConfig(), ConfigDomain.CFG_CANARY_CDC_FREQUENCY, Integer.class, CANARY_CDC_FREQUENCY);

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
    }

    /**
     * Executes the primary logic of the CanaryController in a separate thread.
     * This method is invoked when the thread starts and operates in a continuous loop
     * while the `running` flag remains true. It performs the following:
     * <p>
     * - Logs the start of the CanaryController.
     * - Refreshes the configuration if necessary by invoking the `getConfig` method.
     * - Enters a loop where it:
     * - Executes a canary session via the `runCanarySession` method.
     * - Puts the thread to sleep using the `sleepThread` method to control execution timing.
     * <p>
     * The loop runs indefinitely until the `running` flag is set to false.
     */
    @Override
    public void run() {
        LOG.info("Starting CanaryController");

        while (running) {
            getCanaryConfiguration();
            runCanarySession();
            waitMilliSeconds(canaryIntervalMs, "canary");
        }
    }

    /**
     * Executes a single instance of a canary session in a separate thread.
     * <p>
     * This method performs the following steps:
     * 1. Initializes a new instance of the CanarySession with the necessary dependencies.
     * 2. Creates a thread using the CanarySession instance and assigns the thread a name retrieved from the session.
     * 3. Stores the session-thread pair in the `sessions` map for tracking active sessions.
     * 4. Starts the thread to execute the canary session logic.
     * 5. Waits for the execution of the thread to complete using `thread.join()`.
     * 6. Handles interruptions gracefully by invoking the `handleInterruptedException` method.
     * <p>
     * The method ensures that individual canary sessions are executed in isolation while maintaining
     * the ability to track and control their state via the `sessions` map.
     */
    private void runCanarySession() {
        CanarySession canarySession = new CanarySession(this, cdcProcessor, aiconTosConnectionStatusProducer,
                httpConfig, transformer, getN4Scope(), doCDCCheck());
        Thread thread = new Thread(canarySession, canarySession.getThreadName(threadCounter++));
        sessions.put(canarySession, thread);
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e) {
            handleInterruptedException(e);
        }
        cdcCheckCounter++;
    }

    public boolean isRunning() {
        return running;
    }

    private boolean doCDCCheck() {
        if (cdcCheckCounter >= canaryCDCFrequency) {
            cdcCheckCounter = 0;
            LOG.info("Doing CDC check");
            return true;
        }
        return false;
    }

    private void handleInterruptedException(InterruptedException e) {
        LOG.error("Thread interrupted", e);
        Thread.currentThread().interrupt(); // Preserve interrupt status
    }

    public void stopController() {
        LOG.info("Stopping CanaryController for {}", getCanaryCheckConfig().getName());
        running = false;
    }

    public String toString() {
        return String.format("CanaryController: %s", getCanaryCheckConfig());
    }

    static final String REPORT_TEMPLATE =
            "CanaryController %s, threadState=%s, endPoint=%s, interval/cdc freq=%d/%d\n\tSessionsStats: #ACTIVE=%s, DONE=%s, FAILED=%s \n\t\t%s";
    static final String NO_ENDPOINT_FOUND_MESSAGE = "No Endpoint connector found (bad reference)";

    public String report() {

        Thread.State threadState = Thread.currentThread().getState();
        String endpointInfo = getEndpointInfo();
        int activeSessions = sessions.size();
        Long doneSessions = stats.get(FlowSession.SessionState.DONE);
        Long failedSessions = stats.get(FlowSession.SessionState.FAILED);
        String lastSessionMsg = getLastSessionMsg();

        return splitReportLinesIfTooLong(String.format(REPORT_TEMPLATE,
                this.getName(), threadState, endpointInfo, canaryIntervalMs / 1000, canaryCDCFrequency,
                activeSessions, doneSessions, failedSessions, lastSessionMsg));
    }

    private String getEndpointInfo() {
        if (getHttpConfig() == null) {
            return NO_ENDPOINT_FOUND_MESSAGE;
        }
        return String.format("%s:%s", getHttpConfig().getType(), getHttpConfig().getName());
    }

    public synchronized void sessionEnded(FlowSession flowSession) {
        LOG.info("{} Ended.", flowSession);
        flowSession.setSessionFinishedTs();
        stats.put(flowSession.getState(), stats.get(flowSession.getState()) + 1);
        sessions.remove(flowSession);
        sessionHistoryManager.addSession(flowSession);
        sessionHistoryManager.getAllSessions().stream().forEach(session -> {
            LOG.info("Session {} heeft state {}",
                    session.getSessionType(), session.getState());
        });
    }

    /**
     * Retrieves a message describing the most recent session's state.
     * If no sessions have been executed yet, a default message is returned.
     *
     * @return A string representation of the last session's details, including
     */
    public String getLastSessionMsg() {
        FlowSession lastSession = sessionHistoryManager.peekLatest();
        if (lastSession == null) {
            return "No sessions yet.";
        } else {
            return String.format("Last session finished @ %s, State=%s %s",
                    sdf.format(lastSession.getSessionFinishedTs()),
                    lastSession.getState(),
                    lastSession.getMessage() == null || lastSession.getMessage().isEmpty() ? "" :
                            ", error=" + lastSession.getMessage() + ", reason:" + lastSession.getSessionFailureReason()
            );
        }
    }

    /**
     * Retrieves the last sessions executed, filtered to include only instances of {@code CanarySession}.
     * The method collects these sessions into a {@link Deque} for ordered access.
     *
     * @return a {@link Deque} of {@code CanarySession} objects representing the historical sessions.
     */
    public Deque<CanarySession> getLastSessions() {
        return sessionHistoryManager.getAllSessions().stream()
                .filter(CanarySession.class::isInstance)
                .map(CanarySession.class::cast)
                .collect(Collectors.toCollection(ArrayDeque::new));
    }

    public String getLatestTOSCDCData() {
        return Objects.requireNonNullElse(latestTosCDCData, "");
    }

    public void setLatestTOSCDCData(String cdcDataStr) {
        this.latestTosCDCData = cdcDataStr;
    }

    public String getLatestAiconCDCData() {
        return Objects.requireNonNullElse(latestAiconCDCData, "");
    }

    public void setLatestAiconCDCData(String cdcDataStr) {
        this.latestAiconCDCData = cdcDataStr;
    }

    public void setLatestAiconCDCData(List<CDCData> cdcData) {
        setLatestAiconCDCData(cdcProcessor.convertCDCDataToString(cdcData));
    }

    public FlowSessionManager getSessionManager() {
        return this.sessionHistoryManager;
    }

    public void setTimeSync(Long timeSync) {
        getDataStore().setTimeSync(timeSync);
    }

    public CDCController getCDCController() {
        return getFlowManager().getCDCController();
    }

    public CDCDataProcessor getCDCDataProcessor() {
        return this.cdcProcessor;
    }

    protected String getName() {
        return "CanaryController";
    }

    public Boolean getCdcOk() {
        return this.cdcOk;
    }

    public void setCdcOk(Boolean cdcOk) {
        this.cdcOk = cdcOk;
    }
}
