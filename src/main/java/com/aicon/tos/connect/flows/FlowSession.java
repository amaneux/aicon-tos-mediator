package com.aicon.tos.connect.flows;

import com.aicon.tos.shared.config.ConfigGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

/**
 * Abstract base class for all FlowSession implementations.
 * A FlowSession represents a unit of work in the TOS integration flow.
 */
public abstract class FlowSession implements Runnable {

    protected static final Logger LOG = LoggerFactory.getLogger(FlowSession.class);

    protected AbstractSessionController controller;
    protected String sessionFailureReason;
    private SessionState state = SessionState.ACTIVE;
    protected Date sessionFinishedTs;
    private boolean running = true;
    private String sessionType;
    protected ConfigGroup httpConfig;
    protected ConfigGroup flowConfig;
    protected ConfigGroup kafkaConfig;
    private String message;

    public enum SessionState { DONE, FAILED, ACTIVE }

    protected FlowSession(AbstractSessionController controller, String sessionType,
                          ConfigGroup httpConfig, ConfigGroup flowConfig, ConfigGroup kafkaConfig) {
        this.controller = controller;
        this.sessionType = sessionType;
        this.httpConfig = httpConfig;
        this.flowConfig = flowConfig;
        this.kafkaConfig = kafkaConfig;
    }

    public SessionState getState() {
        return state;
    }

    protected void setState(SessionState state) {
        this.state = state;
    }

    public String getSessionType() {
        return sessionType;
    }

    public void setSessionType(String sessionType) {
        this.sessionType = sessionType;
    }

    public Date getSessionFinishedTs() {
        return sessionFinishedTs;
    }

    public void setSessionFinishedTs() {
        this.sessionFinishedTs = new Date();
    }

    public String getMessage() {
        return message != null ? message : "";
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSessionFailureReason() {
        return sessionFailureReason;
    }

    public void setSessionFailureReason(String sessionFailureReason) {
        if (this.sessionFailureReason == null) {
            this.sessionFailureReason = sessionFailureReason;
        } else {
            this.sessionFailureReason += ", " + sessionFailureReason;
        }
    }

    public boolean isRunning() {
        return running;
    }

    void setRunning(boolean running) {
        this.running = running;
    }

    @Override
    public final void run() {
        LOG.info("Starting {} session", sessionType);
        try {
            execute();
        } catch (FlowSessionException e) {
            setSessionFailureReason(e.getClass().getSimpleName() + ": " + e.getMessage());
            LOG.error(sessionType + " failed", e);
        }

        if (sessionFailureReason != null && !sessionFailureReason.isEmpty()) {
            setState(SessionState.FAILED);
            setMessage(sessionType + " failed: ");
        } else {
            setState(SessionState.DONE);
            setMessage("");
        }

        try {
            sendConnectionStatusMessageIfAvailable();
        } catch (Exception e) {
            LOG.warn("Could not send connection status message", e);
        }

        setSessionFinishedTs();
            if (controller != null) {
                controller.setSessionState(getState(), getMessage() +", reason: " + getSessionFailureReason());
                controller.sessionEnded(this);
            }

            LOG.info("{} session finished", sessionType);
        }

    /**
     * Subclasses must implement this to define their core behavior.
     */
    protected abstract void execute() throws FlowSessionException;

    /**
     * Used to name session threads uniquely.
     */
    protected abstract String getThreadName(int threadCounter);

    /**
     * Optional hook to send a connection status message.
     */
    protected void sendConnectionStatusMessageIfAvailable() {
        // Default: do nothing
    }
}
