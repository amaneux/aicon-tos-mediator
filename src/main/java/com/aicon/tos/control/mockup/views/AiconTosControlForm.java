package com.aicon.tos.control.mockup.views;

import com.aicon.tos.connect.web.pages.MainLayout;
import com.aicon.tos.shared.kafka.AiconTosControlConsumer;
import com.aicon.tos.shared.kafka.AiconTosControlMessageListener;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * The AiconTosControlForm class represents a UI component that displays the operating mode for AICON and TOS.
 * It listens for messages from Kafka via a consumer to update the operating mode in real-time.
 * Additionally, the form triggers email notifications to configured employees when the operating mode changes.
 */
@PageTitle("Aicon Tos Control")
@Route(value = "aiconTosControl-view", layout = MainLayout.class)
public class AiconTosControlForm implements AiconTosControlMessageListener {

    private static final String UNCHANGED_INPUTS_TXT = "Unchanged inputs";
    private static final String TITLE = KafkaConfig.AICON_TOS_CONTROL_TOPIC;

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlForm.class);

    private final Div content;
    private final AiconTosControlConsumer tosControlConsumer;
    private final TextField operatingMode;
    private final TextField timeSync;
    private final TextField comment;
    private final Checkbox ignore;

    private final H3 formTitle;
    private UI ui;

    /**
     * Constructor that initializes the form with necessary components and starts the Kafka consumer.
     *
     * @param aiconTosControlConsumer The Kafka consumer responsible for receiving AICON TOS control messages.
     */
    AiconTosControlForm(AiconTosControlConsumer aiconTosControlConsumer) {
        Thread.currentThread().setName("AiconTosControlForm");

        UI.getCurrent().getPage().setTitle("Aicon TOS Control");

        // Create and configure the title
        formTitle = new H3(TITLE + " " + getUpdateLabel());
        formTitle.setWidthFull();

        // Create labels and text fields
        Span operatingModeLabel = new Span("Operating mode for AICON and TOS");
        operatingMode = new TextField();
        operatingMode.setReadOnly(true);

        ignore = new Checkbox("Ignore changes for AICON and TOS", true);
        ignore.setReadOnly(true);

        Span timeSyncLabel = new Span("TimeSync value between AICON and TOS");
        timeSync = new TextField();
        timeSync.setReadOnly(true);
        timeSync.setWidth("700px");

        Span commentLabel = new Span("Comment");
        comment = new TextField();
        comment.setReadOnly(true);
        comment.setWidth("1000px");

        // Create layout rows for the form fields
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row1.add(operatingModeLabel, operatingMode);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row2.add(timeSyncLabel, timeSync);

        HorizontalLayout row3 = new HorizontalLayout();
        row3.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row3.add(ignore);

        HorizontalLayout row4 = new HorizontalLayout();
        row4.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row4.add(commentLabel, comment);

        // Organize the form fields in a vertical layout
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        formLayout.add(row1, row2, row3, row4);

        // Set up the form content
        content = new Div();
        content.add(formTitle, formLayout);
        content.setWidthFull();
        content.setWidth(300, Unit.MM);

        content.addAttachListener(this::onAttach);

        // Assign the Kafka consumer and add this form as a message listener
        tosControlConsumer = aiconTosControlConsumer;
        tosControlConsumer.addMessageListener(this);

        // Start the Kafka consumer in a separate thread
        startConsumer();

    }

    private void onAttach(AttachEvent event) {
        this.ui = event.getUI();
        LOG.info("AiconTosControlForm attached to UI: {}", ui);
    }

    /**
     * Returns the current form content as a `Div`.
     *
     * @return the `Div` containing the form's components.
     */
    public Div getForm() {
        return content;
    }

    /**
     * Starts a new thread to run the Kafka consumer for message processing.
     */
    private void startConsumer() {
        Thread consumerThread = new Thread(this::run);
        consumerThread.setDaemon(false);
        consumerThread.start();
    }

    private void run() {
        // Initialize and start the Kafka consumer
        tosControlConsumer.initializeConsumer(KafkaConfig.getConsumerProps());
        tosControlConsumer.consumeMessages();
    }

    /**
     * Handles messages received from the Kafka consumer and updates the form's display with new values.
     *
     * @param operatingModeStr The new operating mode received.
     * @param timeSyncStr      The new time synchronization value.
     */
    @Override
    public void onMessageReceived(String operatingModeStr, String timeSyncStr, String comment, Boolean ignore) {
        LOG.debug("Received new operatingMode = {}", operatingModeStr);

        // Retrieve the UI instance safely
        UI currentUI = ui != null && ui.isAttached() ? ui : UI.getCurrent();

        if (currentUI != null && currentUI.isAttached()) {
            currentUI.access(() -> {
                try {
                    refreshContent(operatingModeStr, timeSyncStr, comment, ignore);

                    // Show a notification safely within the UI context
                    Notification.show("New operating mode: " + operatingModeStr, 5000, Notification.Position.MIDDLE);
                } catch (Exception e) {
                    LOG.error("Error while updating UI: {}", e.getMessage(), e);
                }
            });
        } else {
            LOG.warn("UI is detached or unavailable, skipping update.");
        }
    }

    private void refreshContent(String operatingModeStr, String timeSyncStr, String comment, Boolean ignore) {
        // Update the form's fields with the new values
        this.operatingMode.setValue(operatingModeStr);
        this.ignore.setValue(ignore);
        this.timeSync.setValue(timeSyncStr);
        if (!UNCHANGED_INPUTS_TXT.equalsIgnoreCase(comment) || this.comment.getValue().isEmpty()) {
            this.comment.setValue(comment);
        }
        this.formTitle.setText(TITLE + " " + getUpdateLabel());

    }

    /**
     * Generates a timestamp indicating the current date and time for the title.
     *
     * @return A formatted timestamp string.
     */
    private String getUpdateLabel() {
        return "(Last update: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")";
    }
}