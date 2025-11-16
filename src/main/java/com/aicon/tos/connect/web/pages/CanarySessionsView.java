package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.flows.CanaryController;
import com.aicon.tos.connect.flows.CanarySession;
import com.aicon.tos.connect.flows.FlowManager;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.shared.ui.Transport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Deque;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.aicon.tos.shared.util.HtmlUtil.escapeHtml;

@Route(value = "CanarySessionsView", layout = MainLayout.class)
@PageTitle("Mediator Canary Sessions")
public class CanarySessionsView extends VerticalLayout {

    protected static final Logger LOG = LoggerFactory.getLogger(CanarySessionsView.class);

    private final UI ui;
    private final Grid<CanarySession> sessionGrid;
    private final transient CanaryController canaryController;
    private transient ScheduledExecutorService autoRefreshService = null;
    private final transient DataStore dataStore;

    public CanarySessionsView() {
        this.ui = UI.getCurrent();
        dataStore = DataStore.getInstance();

        this.canaryController = FlowManager.getInstance().getCanaryController();
        setPadding(true);

        HorizontalLayout layout = new HorizontalLayout();
        layout.setDefaultVerticalComponentAlignment(Alignment.CENTER);

        Span label = new Span("Number of sessions:");
        label.setWidthFull();

        sessionGrid = new Grid<>(CanarySession.class, false);
        sessionGrid.addColumn(CanarySession::getSessionType).setHeader("Type").setWidth("200px").setFlexGrow(0);
        sessionGrid.addColumn(CanarySession::getSessionFinishedTs).setHeader("Finished").setWidth("300px").setFlexGrow(0);
        sessionGrid.addColumn(CanarySession::getState).setHeader("State").setWidth("100px").setFlexGrow(0);
        sessionGrid.addColumn(new ComponentRenderer<>(session -> {
            Div messageDiv = new Div();

            String msg = escapeHtml(session.getMessage());
            String reason = escapeHtml(session.getSessionFailureReason());

            StringBuilder html = new StringBuilder();
            html.append(msg.replace("\n", "<br>"));

            if (!msg.isEmpty() && !reason.isEmpty()) {
                html.append("<br>");
            }
            html.append(reason.replace("\n", "<br>"));

            messageDiv.getElement().setProperty("innerHTML", html.toString());
            messageDiv.getStyle()
                    .set("white-space", "normal")
                    .set("word-wrap", "break-word")
                    .set("font-size", "12px")
                    .set("max-width", "600px");
            return messageDiv;
        })).setHeader("Message");


        sessionGrid.setWidthFull();
        sessionGrid.setAllRowsVisible(true);

        layout.add(label, getNumberField());
        layout.setSpacing(true);
        add(layout, sessionGrid);

        configurePush();
        refreshTable();

        startAutoRefresh();
    }

    private NumberField getNumberField() {
        int minSize = 5;
        int maxSize = 100;
        dataStore.setSessionsRecordedSize(15);
        int initialSize = dataStore.getSessionsRecordedSize();
        if (initialSize < minSize) {
            initialSize = minSize;
            dataStore.setSessionsRecordedSize(initialSize);
            canaryController.getSessionManager().setSize(initialSize);
        }
        NumberField numberOfSessions = new NumberField();
        numberOfSessions.setValue((double) initialSize);
        numberOfSessions.setStepButtonsVisible(true);
        numberOfSessions.setMin(minSize);
        numberOfSessions.setMax(maxSize);
        numberOfSessions.setStep(1);

        numberOfSessions.addValueChangeListener(event -> {
            Integer value = event.getValue() == null ? null : event.getValue().intValue();
            if (value == null || value < 5 || value > 100) {
                Notification.show("Value must be between 5 and 100.", 3000, Notification.Position.MIDDLE);
            } else {
                dataStore.setSessionsRecordedSize(value);
                canaryController.getSessionManager().setSize(value);
            }
        });
        return numberOfSessions;
    }

    private void refreshTable() {
        Deque<CanarySession> sessionHistory = canaryController.getLastSessions();
        sessionGrid.setItems(sessionHistory);
    }

    private void startAutoRefresh() {
        autoRefreshService = Executors.newSingleThreadScheduledExecutor();
        autoRefreshService.scheduleAtFixedRate(() -> {
            try {
                ui.access(this::refreshTable);
            } catch (Exception e) {
                LOG.error("Error during auto-refresh", e);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (autoRefreshService != null && !autoRefreshService.isShutdown()) {
            autoRefreshService.shutdownNow();
        }
    }

    private void configurePush() {
        ui.getPushConfiguration().setPushMode(PushMode.AUTOMATIC);
        ui.getPushConfiguration().setTransport(Transport.WEBSOCKET_XHR);
    }
}
