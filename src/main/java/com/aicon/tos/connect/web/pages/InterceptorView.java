package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.web.AppComposer;
import com.aicon.tos.interceptor.MessageMeta;
import com.avlino.common.Constants;
import com.avlino.common.KeyValue;
import com.avlino.common.datacache.FixedSizeObservableList;
import com.avlino.common.utils.DateTimeUtils;
import com.avlino.common.utils.StringUtils;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * This class represents the main UI layout for the AICON Interceptor progress view.
 * It shows the status of the last n messages being processed and completed by the interceptor.
 */
@PageTitle("Interceptor messages")
@Route("interceptor-view")
public class InterceptorView extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(InterceptorView.class);

    private UI callBackUI;

    private Grid<MessageMeta> grid;
    private ListDataProvider<MessageMeta> dataProvider;
    private Checkbox autoRefresh;
    private Button butRefresh;

    /**
     * Constructor that connects to {@link com.aicon.tos.interceptor.decide.InterceptorDecide} to get the list of
     * meta information from the messages being processed and completed.
     */
    public InterceptorView() {
        callBackUI = UI.getCurrent();
        Thread.currentThread().setName(InterceptorView.class.getSimpleName());

        UI.getCurrent().addDetachListener(event ->
            LOG.info("UI detached of Interceptor: {}", event.getSource())
            // Resource cleanup of specific actions
        );

        // Configure the UI
        UI.getCurrent().setPollInterval(5000); // 5 seconds
        setSizeFull();
        HorizontalLayout hlControl = ViewHelper.createHorizontalLayout();
        autoRefresh = ViewHelper.createCheckBox("Auto refresh", true);
        butRefresh = ViewHelper.createButton("Refresh", null, Key.F5, ButtonVariant.LUMO_PRIMARY, null);
        butRefresh.addClickListener(event -> dataProvider.refreshAll());

        hlControl.add(butRefresh, autoRefresh);

        createGrid();
        add(new H3("Intercepted messages (being) processed:"), hlControl, grid);
    }


    private Grid<MessageMeta> createGrid() {
        grid = new Grid<>(MessageMeta.class, false);
        grid.setEnabled(true);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);

        FixedSizeObservableList<MessageMeta> list = (FixedSizeObservableList) AppComposer.getInstance().getInterceptorDecide().getMetaCache();
        dataProvider = new ListDataProvider<>(list);

        // Use UI.getCurrent() instead of callBackUI to ensure the UI is still attached
        // and to avoid potential NullPointerException or stale UI reference.
        list.addObserver(event -> {
            if (callBackUI != null && !callBackUI.isClosing()) { // Check if UI is still valid
                callBackUI.access(() -> {
                    if (Boolean.TRUE.equals(autoRefresh.getValue())) {
                        dataProvider.refreshAll();
                        LOG.info("Auto-refreshed grid, list size: {}", list.size());
                    } else {
                        LOG.info("Auto-refresh is off");
                    }
                });
            } else {
                LOG.warn("UI is not available, cannot refresh grid.");
            }
        });
        grid.setItems(dataProvider);        // todo where we do this, is very sensitive regarding observer and column width, not sure yet what the problem is.

        String[] headers = new String[]{"Entity", "Offset", "Received", "Duration(ms)", "Progress", "Content", "Result"};
        grid.addColumn(MessageMeta::getActionEntityCombo)   .setHeader(headers[0]).setWidth( "6%").setResizable(true);
        grid.addColumn(MessageMeta::getOffset)              .setHeader(headers[1]).setWidth( "8%").setResizable(true);
        grid.addColumn(MessageMeta::getCdcReceivedDowTime)  .setHeader(headers[2]).setWidth( "8%").setResizable(true)
            .setTooltipGenerator(meta -> meta.getAllTimeStampsToString(false, DateTimeUtils.TIME_MS_FORMAT, true));
        grid.addColumn(MessageMeta::getDurationMs)          .setHeader(headers[3]).setWidth( "8%").setResizable(true);
        grid.addColumn(MessageMeta::getProgress)            .setHeader(headers[4]).setWidth("10%").setResizable(true);
        grid.addColumn(MessageMeta::getAllEntityValues)     .setHeader(headers[5]).setWidth("30%").setResizable(true);
        grid.addColumn(MessageMeta::getResult)              .setHeader(headers[6]).setWidth("30%").setResizable(true);
        return grid;
    }

    /**
     * Opens dialog does not work, after the first attempt, without showing the dialog, it logs an error:
     * "Ignored listener invocation for click event from the client side for an inert span element"
     *
     * @param meta
     */
    private void openDetailsDialog(MessageMeta meta) {
        try {
            LOG.info("Dialog started for offset {}", meta.getOffset());
            Dialog dialog = new Dialog();
            dialog.setModal(true);

            dialog.setHeaderTitle("Process timing for offset " + meta.getOffset());

            // Create a form layout
            List<KeyValue> timeTable = new ArrayList<>();
            for (Map.Entry<String, Instant> entry : meta.getAllTimeStamps().entrySet()) {
                timeTable.add(new KeyValue(entry.getKey(), String.class, DateTimeUtils.formatInSystemZone(entry.getValue(), DateTimeUtils.DATE_TIME_MS_FORMAT)));
            }

            FormLayout form = new FormLayout();
            Grid grid = ViewHelper.createKeyValueGrid(new String[]{"Event", "Timestamp"}, null, true, false);
            grid.setItems(timeTable);

            form.add(grid);

            // Add the form to the dialog
            dialog.add(form);

            // Add a close button
            Button closeButton = new Button("Close", e -> dialog.close());
            dialog.getFooter().add(closeButton);

            dialog.open();
            UI.getCurrent().push();
            LOG.info("state of dialog is: {}", dialog.isOpened());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
