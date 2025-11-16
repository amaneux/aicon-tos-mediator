package com.aicon.tos.connect.flows;

import com.aicon.tos.connect.cdc.CDCConfig;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import com.aicon.tos.shared.config.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static com.aicon.tos.connect.flows.BaseController.splitReportLinesIfTooLong;
import static com.aicon.tos.shared.util.TimeUtils.waitSeconds;

/**
 * CDCController is responsible for managing and supervising Change Data Capture (CDC)
 * sessions. This class implements the Runnable interface, allowing it to execute its
 * logic within a separate thread. The controller monitors multiple CDC sessions and ensures
 * that they operate as expected while providing functionality to manage, report on, and stop
 * these sessions.
 * <p>
 * Key functionalities include:
 * - Starting CDC sessions for configured topics.
 * - Managing session state and statistics.
 * - Finding the date associated with a specific key in a session's topic.
 * - Generating reports on session statistics.
 */
public class CDCController extends AbstractSessionController implements Runnable, StoppableController {
    private static final Logger LOG = LoggerFactory.getLogger(CDCController.class);

    private static final int THREAD_SLEEP_INTERVAL_SEC = 5;

    // Use thread-safe map for session stats
    private final Map<FlowSession.SessionState, Long> stats;
    final Map<FlowSession, Thread> sessions;

    // Instance variables
    private volatile boolean running = true; // volatile ensures visibility across threads
    private volatile FlowSession.SessionState sessionState = null;
    private volatile String sessionStateMsg = null;
    private final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd hh:mm:ss.SSS");
    private CDCDataProcessor cdcProcessor;
    private int threadCounter = 0;

    public CDCController(FlowManager flowManager) {
        super(flowManager);
        LOG.info("Instantiating CDC Controller");
        getConfig();

        // Use thread-safe structures for collections
        this.sessions = new ConcurrentHashMap<>();
        this.stats = new ConcurrentHashMap<>();

        // Initialize stats for each state
        for (FlowSession.SessionState state : FlowSession.SessionState.values()) {
            stats.put(state, 0L);
        }
    }

    /**
     * Initializes the `cdcProcessor` field with a new instance of `CDCDataProcessor`,
     * while fetching necessary configuration settings.
     */
    private void getConfig() {
        this.cdcProcessor = new CDCDataProcessor(getCanaryCheckConfig());       // todo Harrie? Why a canary check config here?

        String kafkaRef = (String) getCanaryCheckConfig().getChildGroup(ConfigType.KafkaRef).getRef();
        this.kafkaConfig = getConnectionsConfig().getReferencedGroup(kafkaRef, ConfigType.Kafka);
        String httpRef = (String) getCanaryCheckConfig().getChildGroup(ConfigType.HttpRef).getRef();
        this.httpConfig = getConnectionsConfig().getReferencedGroup(httpRef, ConfigType.Http);
    }

    /**
     * Executes the main logic for the CDCController. This method is responsible for:
     * 1. Logging the initialization of the CDCController.
     * 2. Creating and starting a Change Data Capture (CDC) session for each configured topic
     * by invoking the {@code runCDCSession} method.
     * 3. Maintaining the running state of the controller until explicitly stopped, by repeatedly
     * calling the {@code sleepThread} method to avoid CPU overutilization during idle periods.
     * 4. Logging the termination of the CDCController when it finishes execution.
     * <p>
     * The method utilizes the configuration provided by {@code cdcProcessor.getCdcConfigTable()}
     * to identify the topics for which CDC sessions are to be initiated.
     */
    @Override
    public void run() {
        LOG.info("Starting CDCController");

        // Start a CDCSession for each topic
        for (CDCConfig cdcConfig : cdcProcessor.getCdcConfigTable()) {
            LOG.info("Start buffering messages from topic {}", cdcConfig.topicName());
            runCDCSession(cdcConfig.topicName());
        }

        // Keep the controller thread running until stopped
        while (running) {
            waitSeconds(THREAD_SLEEP_INTERVAL_SEC, "CDCController");
        }

        LOG.info("CDC Controller finished running");
    }

    /**
     * Starts and manages a CDC (Change Data Capture) session.
     *
     * @param topicName The name of the topic for which the CDC session is created.
     */
    void runCDCSession(String topicName) {
        CDCSession cdcSession = new CDCSession(this, topicName);
        Thread thread = new Thread(cdcSession, cdcSession.getThreadName(threadCounter++));
        sessions.put(cdcSession, thread);
        thread.start();
    }

    /**
     * Finds the date associated with the given gKey in a session's topic.
     *
     * @param cdcConfig the CDCConfig object containing the topic name
     * @param gKey      the key for which the date needs to be fetched
     * @param threshold the max waiting time in milliseconds
     * @param timeSync  additional synchronization buffer in milliseconds
     * @return the associated date or null if not found within the allowed time
     */
    public Long findDateForGKey(CDCConfig cdcConfig, String gKey, Long threshold, Long timeSync) {
        Long date = null;
        LOG.info("---------- findDateForGKey( {}) ----------", gKey);

        CDCSession currentCDCSession = sessions.keySet()
                .stream()
                .filter(CDCSession.class::isInstance)
                .map(CDCSession.class::cast)
                .filter(session -> session.getTopic().equals(cdcConfig.topicName()))
                .findFirst()
                .orElse(null);

        if (currentCDCSession == null) {
            LOG.warn("No active session found for topic: {}", cdcConfig.topicName());
            return null;
        }

        long waitingSince = Instant.now().toEpochMilli();

        while (date == null && notWaitedTooLong(waitingSince, threshold)) {
            LOG.info("Searching in topic {} for key {}, waited={}ms, max wait={}ms",
                    currentCDCSession.getTopic(),
                    gKey,
                    Instant.now().toEpochMilli() - waitingSince,
                    (threshold + timeSync)
            );

            date = currentCDCSession.findDateForGKey(gKey);

            if (date == null) {
                waitSeconds(2, "topic " + currentCDCSession.getTopic());
            }
        }

        if (date == null) {
            LOG.info("No date found for key {} in topic {}", gKey, currentCDCSession.getTopic());
        } else {
            LOG.info("Date {} found for key {} in topic {}", date, gKey, currentCDCSession.getTopic());
        }
        return date;
    }

    /**
     * Determines if waiting time has exceeded the threshold plus additional timeSync.
     */
    private boolean notWaitedTooLong(Long waitingSince, Long threshold) {
        long now = Instant.now().toEpochMilli();
        boolean result = now - waitingSince <= threshold;
        LOG.info("Waited too long? {}, waited for {} msec, max wait = {}",
                !result, now - waitingSince, threshold);
        return result;
    }

    public void stopController() {
        LOG.info("Stopping CDCController");
        running = false;
        getCDCSessions().keySet().forEach(CDCSession::stopSession);
    }

    public String getName() {
        return "CDC Controller";
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public String toString() {
        return "CDCController";
    }

    public Map<CDCSession, Thread> getCDCSessions() {
        return sessions.entrySet()
                .stream()
                .filter(entry -> entry.getKey() instanceof CDCSession)
                .collect(Collectors.toMap(
                        entry -> (CDCSession) entry.getKey(),
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        ConcurrentHashMap::new
                ));
    }

    public String report() {
        final String REPORT_TEMPLATE =
                "CDC Controller %s, threadState=%s,\n\tSessionsStats: #ACTIVE=%s, DONE=%s, FAILED=%s \n\t\t%s";

        int activeSessions = sessions.size();
        Long doneSessions = stats.get(FlowSession.SessionState.DONE);
        Long failedSessions = stats.get(FlowSession.SessionState.FAILED);

        return splitReportLinesIfTooLong(String.format(REPORT_TEMPLATE,
                this.getName(),
                Thread.currentThread().getState(),
                activeSessions,
                doneSessions,
                failedSessions,
                getLastSessionMsg()
        ));
    }

    public synchronized void sessionEnded(FlowSession flowSession) {
        if (flowSession == null) {
            LOG.warn("Received a null FlowSession in sessionEnded!");
            return;
        }

        LOG.info("Session Ended: {}", flowSession);

        // Set finished timestamp
        flowSession.setSessionFinishedTs();

        // Update stats safely using ConcurrentHashMap's compute method
        stats.compute(flowSession.getState(), (key, value) -> (value == null ? 1 : value + 1));

        // Remove session and handle if not found
        if (sessions.remove(flowSession) == null) {
            LOG.warn("Attempted to remove a non-existing FlowSession: {}", flowSession);
        }
    }

    public void setSessionState(FlowSession.SessionState state, String msg) {
        this.sessionState = state;
        this.sessionStateMsg = msg;
    }

    public String getLastSessionMsg() {
        synchronized (this) { // Ensure thread safety
            if (sessionState == null) {
                return "No sessions completed yet.";
            }

            String safeState = sessionState != null ? sessionState.name().replace("%", "%%") : "UNKNOWN";
            String safeMessage = (sessionStateMsg != null && !sessionStateMsg.isEmpty())
                    ? ", error=" + sessionStateMsg.replace("%", "%%")
                    : "";

            return String.format("Last session finished @ %s, State=%s%s",
                    sdf.format(new Date()),  // Replace with stored timestamp if needed
                    safeState,
                    safeMessage);
        }
    }
}