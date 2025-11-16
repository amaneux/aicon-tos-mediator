package com.aicon.tos.shared.util;

import com.aicon.tos.control.OperatingModeRuleEngine;
import com.aicon.tos.control.OperatingModeRuleOld;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.aicon.tos.shared.schema.OperatingMode;
import com.aicon.tos.shared.schema.UserOperatingMode;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A utility class that provides methods to generate, sort, and manage a table of different
 * operating modes based on the combination of several input parameters.
 */
public class RefactorOperatingModeRuleTable {

    static List<OperatingModeRuleOld> rows = new ArrayList<>();

    /**
     * Generates a list of rows representing various combinations where the first four parameters vary.
     */
    private static void getListWithVaryingFirst4Parameters() {
        for (ConnectionStatus currentConnectionStatus : ConnectionStatus.values()) {
            for (ConnectionStatus newConnectionStatus : ConnectionStatus.values()) {
                for (Boolean currentCdcOk : List.of(Boolean.TRUE, Boolean.FALSE)) {
                    for (Boolean newCdcOk : List.of(Boolean.TRUE, Boolean.FALSE)) {
                        // Skip if there is no change in connection status and CDC status
                        if (newConnectionStatus == currentConnectionStatus && newCdcOk.equals(currentCdcOk)) {
                            continue;
                        }
                        for (UserOperatingMode userOperatingMode : UserOperatingMode.values()) {
                            add(currentConnectionStatus, newConnectionStatus, currentCdcOk, newCdcOk,
                                userOperatingMode, userOperatingMode, OperatingMode.OFF);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates a list of rows representing combinations where the first four parameters are constant,
     * and the last two parameters vary.
     */
    private static void getListWithVaryingLast2Parameters() {
        for (ConnectionStatus connectionStatus : ConnectionStatus.values()) {
            for (boolean cdcOk : new boolean[]{true, false}) {
                for (UserOperatingMode currentUserOperatingMode : UserOperatingMode.values()) {
                    for (UserOperatingMode userOperatingMode : UserOperatingMode.values()) {
                        // Skip if the new user operating mode is the same as the current one
                        if (userOperatingMode == currentUserOperatingMode) {
                            continue;
                        }
                        rows.add(new OperatingModeRuleOld(
                                connectionStatus,
                                connectionStatus,
                                cdcOk,
                                cdcOk,
                                currentUserOperatingMode,
                                userOperatingMode,
                                OperatingMode.OFF
                        ));
                    }
                }
            }
        }
    }

    /**
     * Generates a complete list by combining different rows with varying parameters
     * and then updates the rows in the list with specific operating modes if a match is found.
     */
    private static void getCompleteList() {
        rows.clear();

        getListWithVaryingFirst4Parameters();
        getListWithVaryingLast2Parameters();

        List<OperatingModeRuleOld> allRows = new ArrayList<>(rows);

        rows.clear();
        OperatingModeRuleEngine operatingModeRuleEngine = OperatingModeRuleEngine.getInstance(true);
//        rows.addAll(operatingModeRuleEngine.getRules());

        updateAllRowsWithRows(allRows);

        rows.clear();
        rows.addAll(allRows);
    }

    /**
     * Updates the list of all rows by copying the operating mode from matching rows in another list.
     *
     * @param allRows The list of all rows to be updated.
     */
    private static void updateAllRowsWithRows(List<OperatingModeRuleOld> allRows) {
        for (OperatingModeRuleOld allRow : allRows) {
            for (OperatingModeRuleOld row : rows) {
                if (allRow.getCurrentConnectionStatus().equals(row.getCurrentConnectionStatus()) &&
                        allRow.getNewConnectionStatus().equals(row.getNewConnectionStatus()) &&
                        allRow.getCurrentCdcOk().equals(row.getCurrentCdcOk()) &&
                        allRow.getNewCdcOk().equals(row.getNewCdcOk()) &&
                        allRow.getCurrentUserOperatingMode().equals(row.getCurrentUserOperatingMode()) &&
                        allRow.getNewUserOperatingMode().equals(row.getNewUserOperatingMode())) {
                    allRow.setNewOperatingMode(row.getNewOperatingMode());
                    break;
                }
            }
        }
    }

    /**
     * Adds a new row to the list with the specified parameters.
     *
     * @param currentConnectionStatus  The current connection status.
     * @param newConnectionStatus      The new connection status.
     * @param currentCdcOk             The current CDC status.
     * @param newCdcOk                 The new CDC status.
     * @param currentUserOperatingMode The current user operating mode.
     * @param newUserOperatingMode     The new user operating mode.
     * @param newOperatingMode         The resulting operating mode.
     */
    private static void add(ConnectionStatus currentConnectionStatus, ConnectionStatus newConnectionStatus,
                            Boolean currentCdcOk, Boolean newCdcOk,
                            UserOperatingMode currentUserOperatingMode, UserOperatingMode newUserOperatingMode,
                            OperatingMode newOperatingMode) {
        rows.add(new OperatingModeRuleOld(currentConnectionStatus, newConnectionStatus,
                                          currentCdcOk, newCdcOk,
                                          currentUserOperatingMode, newUserOperatingMode,
                                          newOperatingMode));
    }

    /**
     * The main method to generate, sort, and print the list of rows.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        getCompleteList();

        // Sort the list based on columns, starting from column 6, 5, 4, etc.
        rows.sort(Comparator
                          .comparing(OperatingModeRuleOld::getCurrentConnectionStatus)
                          .thenComparing(OperatingModeRuleOld::getNewConnectionStatus)
                          .thenComparing(OperatingModeRuleOld::getCurrentCdcOk)
                          .thenComparing(OperatingModeRuleOld::getNewCdcOk)
                          .thenComparing(OperatingModeRuleOld::getCurrentUserOperatingMode)
                          .thenComparing(OperatingModeRuleOld::getNewUserOperatingMode));

        // Print the sorted list with row numbers
        int i = 0;
        for (OperatingModeRuleOld row : rows) {
            System.out.format("%-10s %s\n", ++i, row);
        }
    }
}
