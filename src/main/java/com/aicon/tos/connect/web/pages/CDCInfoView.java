package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.cdc.CDCData;
import com.aicon.tos.connect.cdc.CDCDataProcessor;
import com.aicon.tos.connect.flows.CanaryController;
import com.aicon.tos.connect.flows.FlowManager;
import com.aicon.tos.shared.util.TimeUtils;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.abs;

@Route(value = "Mediator CDC Information", layout = MainLayout.class)
@PageTitle("CDC Information")
public class CDCInfoView extends FormLayout {

    protected static final Logger LOG = LoggerFactory.getLogger(CDCInfoView.class);

    private final UI ui;
    private final transient CanaryController canaryController;
    private final TextArea tosCDCDataField = new TextArea();
    private final TextArea aiconCDCDataField = new TextArea();
    private final TextArea latenciesField = new TextArea();
    private final TextArea cdcTablesField = new TextArea();
    private final TextArea timeSyncField = new TextArea();
    private transient ScheduledExecutorService autoRefreshService;
    private final transient DataStore dataStore;
    private final transient CDCDataProcessor cdcDataProcessor;


    public CDCInfoView() {
        this.ui = UI.getCurrent();
        this.canaryController = FlowManager.getInstance().getCanaryController();
        this.cdcDataProcessor = canaryController.getCDCDataProcessor();
        dataStore = DataStore.getInstance();

        setResponsiveSteps(new ResponsiveStep("0", 1));
        // Set width of all labels
        this.getStyle().set("--vaadin-form-item-label-width", "200px");

        String width = "800px";

        // Configured CDC Tables
        addFormItem(cdcTablesField, "Configured CDC Tables:");
        cdcTablesField.setValue(canaryController.getCDCDataProcessor().getCdcConfigTableString());
        cdcTablesField.setWidth(width);
        cdcTablesField.setReadOnly(true);

        // Time sync value
        addFormItem(timeSyncField, "Time sync(ms):");
        timeSyncField.setValue(explainTimeSync(dataStore.getTimeSync()));
        timeSyncField.setWidth(width);
        timeSyncField.setReadOnly(true);

        // TOS CDC Data
        addFormItem(tosCDCDataField, "TOS CDC Data:");
        tosCDCDataField.setValue(cdcDataProcessor.convertRawStringToPrettyCDCString(canaryController.getLatestTOSCDCData()));
        tosCDCDataField.setWidth(width);
        tosCDCDataField.setReadOnly(true);

        // AICON CDC Data
        addFormItem(aiconCDCDataField, "AICON CDC Data:");
        aiconCDCDataField.setValue(cdcDataProcessor.convertRawStringToPrettyCDCString(canaryController.getLatestAiconCDCData()));
        aiconCDCDataField.setWidth(width);
        aiconCDCDataField.setReadOnly(true);

        // Differences Data
        addFormItem(latenciesField, "Latencies in CDC Data(ms):");
        latenciesField.setValue(getLatencies(canaryController.getLatestTOSCDCData(), canaryController.getLatestAiconCDCData()));
        latenciesField.setWidth(width);
        latenciesField.setReadOnly(true);

        // add formula description
        Span textLine = new Span("\tLatency  = (Aicon data time - Time sync) - Tos data time");
        FormLayout.FormItem formulaItem = addFormItem(textLine, "");
        formulaItem.getElement().setAttribute("colspan", "2");

        configurePush();
        refreshView();

        startAutoRefresh();
    }

    /**
     * Updates the UI fields with the most recent data from the underlying systems.
     * This method retrieves data from the `canaryController`, `cdcDataProcessor`, and `dataStore`,
     * and formats or processes it as needed before assigning it to the corresponding UI fields.
     * <p>
     * Specifically, this method performs the following actions:
     * - Retrieves the CDC configuration table string from the `canaryController` and sets it to `cdcTablesField`.
     * - Retrieves a time synchronization value from the `dataStore` and sets it to `timeSyncField`.
     * - Converts raw CDC data for TOS and Aicon systems into a human-readable format using the `cdcDataProcessor`
     * and then assigns them to `tosCDCDataField` and `aiconCDCDataField`, respectively.
     * - Calls the `getLatencies` method to compute latencies between tables from the TOS and Aicon CDC data
     * and sets the result to `latenciesField`.
     * <p>
     * This method ensures that the view is kept current with the latest data from the monitored systems,
     * reflecting changes and updates made to those systems.
     */
    private void refreshView() {
        cdcTablesField.setValue(canaryController.getCDCDataProcessor().getCdcConfigTableString());
        timeSyncField.setValue(explainTimeSync(dataStore.getTimeSync()));
        tosCDCDataField.setValue(cdcDataProcessor.convertRawStringToPrettyCDCString(canaryController.getLatestTOSCDCData()));
        aiconCDCDataField.setValue(cdcDataProcessor.convertRawStringToPrettyCDCString(canaryController.getLatestAiconCDCData()));
        String latencies = getLatencies(canaryController.getLatestTOSCDCData(), canaryController.getLatestAiconCDCData());
        latenciesField.setValue(latencies);
    }

    /**
     * Calculates the latency for each table by comparing creation timestamps
     * from two lists of CDCData, one from TOS and another from AICON.
     * Combines the results into a comma-separated string.
     *
     * @param latestTOSCDCData   the most recent CDC data in string format from the TOS system
     * @param latestAiconCDCData the most recent CDC data in string format from the AICON system
     * @return a string containing unique IDs and their corresponding latencies in the format "tableName(gkey)=latency"
     */
    private String getLatencies(String latestTOSCDCData, String latestAiconCDCData) {
        List<CDCData> tosCDCData = cdcDataProcessor.convertStringToCDCData(latestTOSCDCData);
        List<CDCData> aiconCDCData = cdcDataProcessor.convertStringToCDCData(latestAiconCDCData);

        // Create a map from AICON CDC data using the gkey as the key.
        Map<String, CDCData> aiconCDCMap = aiconCDCData.stream()
                .collect(Collectors.toMap(CDCData::getGkey, Function.identity(), (a, b) -> a));

        StringJoiner latencies = new StringJoiner(",\n");

        // Iterate over TOS CDC data and compare using the gkey
        for (CDCData tosCDC : tosCDCData) {
            CDCData aiconCDC = aiconCDCMap.get(tosCDC.getGkey());
            if (aiconCDC != null
                    && tosCDC.getCreationTimestamp() != null
                    && aiconCDC.getCreationTimestamp() != null) {
                long latency = abs((tosCDC.getCreationTimestampAsLong() - aiconCDC.getCreationTimestampAsLong())
                        + dataStore.getTimeSync());
                // Combine table name and gkey to form a unique identifier for output
                String uniqueId = tosCDC.getTableName() + "(" + tosCDC.getGkey() + ")";
                latencies.add(uniqueId + "=" + latency + " (" + TimeUtils.formatMilliseconds(latency) + ")");
            }
        }
        return latencies.toString();
    }

    private String explainTimeSync(long timeSync) {
        String timeSyncStr = Long.toString(timeSync);
        if (timeSync > 0L) {
            timeSyncStr = timeSyncStr + "  (+ = AICON behind TOS)";
        } else {
            timeSyncStr = timeSyncStr + " (- = AICON ahead of TOS)";
        }
        return timeSyncStr;
    }


    /**
     * Starts an automatic refresh mechanism to periodically update the view with the latest data from
     * the underlying systems. This method sets up a scheduled task that invokes the `refreshView`
     * method every 2 seconds.
     * <p>
     * Specifically, this method:
     * - Initializes a `ScheduledExecutorService` with a single thread.
     * - Configures the executor to run the `refreshView` method in the current UI context at fixed intervals.
     * - Uses error handling to log any exceptions that might occur during the view update process.
     * <p>
     * The schedule starts immediately (0-second delay) and repeats every 2 seconds. This ensures that
     * the view is consistently updated with fresh data from the systems being monitored.
     * <p>
     * If any exception occurs during the execution of the scheduled task, the error is logged using
     * the application's logging framework.
     * <p>
     * Note:
     * - This method is intended to be used primarily within the UI lifecycle.
     * - Proper consideration of resource cleanup is required when the parent UI or component is detached.
     */
    private void startAutoRefresh() {
        autoRefreshService = Executors.newSingleThreadScheduledExecutor();
        autoRefreshService.scheduleAtFixedRate(() -> {
            try {
                ui.access(this::refreshView);
            } catch (Exception e) {
                LOG.error("Error during auto-refresh", e);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * Handles the detachment of the component from the UI.
     * This method ensures proper cleanup of resources when the component
     * is no longer part of the UI. Specifically, it terminates the
     * `autoRefreshService` if it is active to prevent resource leaks.
     *
     * @param detachEvent the event triggered when the component is detached
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (autoRefreshService != null && !autoRefreshService.isShutdown()) {
            autoRefreshService.shutdownNow();
        }
    }

    /**
     * Configures the push capabilities for the UI of this view, enabling and defining
     * the mode and transport mechanism for server-client communication updates.
     * <p>
     * This method sets up the following configurations:
     * - PushMode: Configured to `AUTOMATIC`, ensuring server-sent updates are automatically
     * pushed to the client without requiring explicit client requests.
     * - Transport: Configured to use `WEBSOCKET_XHR`, which combines WebSocket as the primary
     * transport mechanism with XMLHttpRequest (XHR) as a fallback for compatibility.
     * <p>
     * This configuration facilitates real-time updates within the UI, making it responsive
     * to changes occurring on the server side.
     */
    private void configurePush() {
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);
    }
}
