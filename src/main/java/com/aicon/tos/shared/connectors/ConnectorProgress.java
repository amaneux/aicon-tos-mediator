package com.aicon.tos.shared.connectors;

import com.aicon.tos.shared.ResultEntry;
import com.aicon.tos.shared.ResultLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Keeps track of the current state of a connector, from IDLE..STOPPED and contains the result of the last
 * state activity with level OK, WARN, ERROR + any free text message.
 */
public class ConnectorProgress {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorProgress.class);

    public enum ConnectorState {
        IDLE        (false, false),
        INITIALISING(false, false),
        INITIALIZED (true , true),
        CONNECTING  (true , true),
        CONNECTED   (true , true),
        RECONNECTING(true , false),
        FAILED      (false, false),
        STOPPING    (false, true),
        STOPPED     (false, false);

        boolean running = false;
        boolean connected = false;

        ConnectorState(boolean running, boolean connected) {
            this.running = running;
            this.connected = connected;
        }

        public boolean isRunning() {
            return running;
        }

        public boolean isConnected() {
            return connected;
        }
    }

    private String name = "Connector";
    private ConnectorState state = ConnectorState.IDLE;
    private ResultEntry result = null;

    public ConnectorProgress() {
    }

    public ConnectorProgress(String name) {
        this.name = name;
    }

    /**
     * Sets the progress of the connector activity.
     * @param connectorState the new state of the connector
     * @param result the result belonging to the last state (when null, will reset any result to {@link ResultLevel#OK}).
     */
    public ConnectorProgress setProgress(ConnectorState connectorState, ResultEntry result) {
        if (this.state != connectorState) {
            log4Level(String.format("%s state changed: %s → %s", this.name, this.state, connectorState));
        }
        this.state = connectorState;
        this.result = result != null ? result : new ResultEntry(ResultLevel.OK);
        return this;
    }

    /**
     * Sets the progress with <code>newState<code/> when currentState == <code >whenState</code>.
     * @param newState the new state to set
     * @param whenState the state to check against the current state.
     */
    public ConnectorProgress setProgressWhen(ConnectorState newState, ConnectorState whenState) {
        return is(whenState) ? setProgress(newState) : this;
    }

    /**
     * Sets the progress with {@link ResultLevel#OK).}
     * @param connectorState the new state to set
     */
    public ConnectorProgress setProgress(ConnectorState connectorState) {
        return setProgress(connectorState, new ResultEntry(ResultLevel.OK));
    }

    /**
     * Sets the progress with given result level, sets any message to null.
     * @param connectorState the new state to set
     * @param level the level to set
     */
    public ConnectorProgress setProgress(ConnectorState connectorState, ResultLevel level) {
        return setProgress(connectorState, new ResultEntry(level));
    }

    /**
     * Sets the progress with level and message.
     * @param connectorState the new state to set
     * @param level the new level to set
     * @param resultText the result text to set
     */
    public ConnectorProgress setProgress(ConnectorState connectorState, ResultLevel level, String resultText) {
        return setProgress(connectorState, new ResultEntry(level, resultText));
    }

    public boolean is(ConnectorState connectorState) {
        return connectorState != null ? state == connectorState : false;
    }

    public boolean in(ConnectorState... inStates) {
        if (inStates == null) {return false;}
        for (ConnectorState state : inStates) {
            if (is(state)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the result without progressing the state
     * @param level the new result level
     * @param resultText the result text
     */
    public ConnectorProgress setResult(ResultLevel level, String resultText) {
        result = new ResultEntry(level, resultText);
        return this;
    }

    /**
     * Sets the result without progressing the state
     * @param level the new result level
     * @param ex the result exception
     */
    public ConnectorProgress setResult(ResultLevel level, Exception ex) {
        result = new ResultEntry(level).setException(ex);
        return this;
    }

    /**
     * Resets the result to OK without progressing the state
     */
    public ConnectorProgress resetResult() {
        result = new ResultEntry(ResultLevel.OK);
        return this;
    }

    public ConnectorState getState() {
        return state;
    }

    /**
     * @return the last result given by the caller; when null will return a new entry with {@link ResultLevel#OK}.
     */
    public ResultEntry getResult() {
        if (result == null) {
            result = new ResultEntry(ResultLevel.OK);
        }
        return result;
    }

    public ResultLevel getResultLevel() {
        return getResult().getLevel();
    }

    public String getResultMessage() {
        return getResult().getMessage();
    }

    public String toString() {
        return String.format("%s [%s]", getState(), getResult());
    }

    private void log4Level(String text) {
        switch(getResult().getLevel()) {
            case OK     -> LOG.info(text);
            case WARN   -> LOG.warn(text);
            case ERROR  -> LOG.error(text);
        }
    }
}
