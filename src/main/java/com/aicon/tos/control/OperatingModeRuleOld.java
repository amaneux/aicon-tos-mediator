package com.aicon.tos.control;

import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;

/**
 * Represents a rule for determining the new operating mode based on the current and new states
 * of connection status, CDC status, and user operating mode.
 */
public class OperatingModeRuleOld {

    // Fields
    private ConnectionStatus currentConnectionStatus;
    private ConnectionStatus newConnectionStatus;
    private Boolean currentCdcOk;
    private Boolean newCdcOk;
    private UserOperatingMode currentUserOperatingMode;
    private UserOperatingMode newUserOperatingMode;
    private OperatingMode newOperatingMode;

    public OperatingModeRuleOld() {
        throw new IllegalStateException("Old class");
    }

    /**
     * Constructs an OperatingModeRule with the given parameters.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status (true if CDC is OK).
     * @param newCdcOk                 The new CDC status (true if CDC is OK).
     * @param currentUserOperatingMode The current user operating mode (ON, OFF, AUTO, SHADOW).
     * @param newUserOperatingMode     The new user operating mode (ON, OFF, AUTO, SHADOW).
     * @param newOperatingMode         The operating mode to set if this rule matches.
     */
    public OperatingModeRuleOld(ConnectionStatus currentConnectionStatus, ConnectionStatus newConnectionStatus,
                                Boolean currentCdcOk, Boolean newCdcOk, UserOperatingMode currentUserOperatingMode,
                                UserOperatingMode newUserOperatingMode, OperatingMode newOperatingMode) {

        throw new IllegalStateException("Old class");

//        this.currentConnectionStatus = currentConnectionStatus;
//        this.newConnectionStatus = newConnectionStatus;
//        this.currentCdcOk = currentCdcOk;
//        this.newCdcOk = newCdcOk;
//        this.currentUserOperatingMode = currentUserOperatingMode;
//        this.newUserOperatingMode = newUserOperatingMode;
//        this.newOperatingMode = newOperatingMode;
    }

    // Getters

    /**
     * @return The current connection status.
     */
    public ConnectionStatus getCurrentConnectionStatus() {
        return currentConnectionStatus;
    }

    /**
     * @return The new connection status.
     */
    public ConnectionStatus getNewConnectionStatus() {
        return newConnectionStatus;
    }

    /**
     * @return The current CDC status (true if CDC is OK).
     */
    public Boolean getCurrentCdcOk() {
        return currentCdcOk;
    }

    /**
     * @return The new CDC status (true if CDC is OK).
     */
    public Boolean getNewCdcOk() {
        return newCdcOk;
    }

    /**
     * @return The current user operating mode (ON, OFF, AUTO, SHADOW).
     */
    public UserOperatingMode getCurrentUserOperatingMode() {
        return currentUserOperatingMode;
    }

    /**
     * @return The new user operating mode (ON, OFF, AUTO, SHADOW).
     */
    public UserOperatingMode getNewUserOperatingMode() {
        return newUserOperatingMode;
    }

    /**
     * @return The new operating mode (ON, OFF, SHADOW).
     */
    public OperatingMode getNewOperatingMode() {
        return newOperatingMode;
    }

    // Setters

    /**
     * Sets the current connection status.
     *
     * @param currentConnectionStatus The current connection status to be set.
     */
    public void setCurrentConnectionStatus(ConnectionStatus currentConnectionStatus) {
        this.currentConnectionStatus = currentConnectionStatus;
    }

    /**
     * Sets the new connection status.
     *
     * @param newConnectionStatus The new connection status to be set.
     */
    public void setNewConnectionStatus(ConnectionStatus newConnectionStatus) {
        this.newConnectionStatus = newConnectionStatus;
    }

    /**
     * Sets the current CDC status.
     *
     * @param currentCdcOk The current CDC status to be set.
     */
    public void setCurrentCdcOk(Boolean currentCdcOk) {
        this.currentCdcOk = currentCdcOk;
    }

    /**
     * Sets the new CDC status.
     *
     * @param newCdcOk The new CDC status to be set.
     */
    public void setNewCdcOk(Boolean newCdcOk) {
        this.newCdcOk = newCdcOk;
    }

    /**
     * Sets the current user operating mode.
     *
     * @param currentUserOperatingMode The current user operating mode to be set.
     */
    public void setCurrentUserOperatingMode(UserOperatingMode currentUserOperatingMode) {
        this.currentUserOperatingMode = currentUserOperatingMode;
    }

    /**
     * Sets the new user operating mode.
     *
     * @param newUserOperatingMode The new user operating mode to be set.
     */
    public void setNewUserOperatingMode(UserOperatingMode newUserOperatingMode) {
        this.newUserOperatingMode = newUserOperatingMode;
    }

    /**
     * Sets the new operating mode.
     *
     * @param newOperatingMode The new operating mode to be set.
     */
    public void setNewOperatingMode(OperatingMode newOperatingMode) {
        this.newOperatingMode = newOperatingMode;
    }

    // Logic

    /**
     * Determines if this rule matches the given parameters.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status (true if OK).
     * @param newCdcOk                 The new CDC status (true if OK).
     * @param currentUserOperatingMode The current user operating mode.
     * @param newUserOperatingMode     The new user operating mode.
     * @return True if all parameters match this rule, otherwise false.
     */
    public Boolean matches(ConnectionStatus currentConnectionStatus, ConnectionStatus newConnectionStatus,
                           Boolean currentCdcOk, Boolean newCdcOk, UserOperatingMode currentUserOperatingMode,
                           UserOperatingMode newUserOperatingMode) {
        return this.currentConnectionStatus == currentConnectionStatus &&
                this.newConnectionStatus == newConnectionStatus &&
                this.currentCdcOk.equals(currentCdcOk) &&
                this.newCdcOk.equals(newCdcOk) &&
                this.currentUserOperatingMode == currentUserOperatingMode &&
                this.newUserOperatingMode == newUserOperatingMode;
    }
}
