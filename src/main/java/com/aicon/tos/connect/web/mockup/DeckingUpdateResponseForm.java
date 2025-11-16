package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateMessageListener;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateResponseConsumer;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class represents a form for configuring and sending AICON TOS connection status messages.
 * It includes fields for setting request details, timestamps, communication status, and CDC information.
 */
public class DeckingUpdateResponseForm implements AiconYardDeckingUpdateMessageListener {

    private static final Logger LOG = LoggerFactory.getLogger(DeckingUpdateResponseForm.class);

    private static final String TITLE = "AICON_YARD_DECKING_UPDATE_RESPONSE";
    private AiconYardDeckingUpdateResponseConsumer consumer = null;

    private H3 formTitle;
    private UI ui;
    private Div content = null;

    private final TextField resultCodeField;
    private final TextArea resultTextField;
    private final TextField resultAudienceField;
    private final TextField resultLevelField;

    protected DeckingUpdateResponseForm(AiconYardDeckingUpdateResponseConsumer consumer) {
        this.consumer = consumer;

        Thread.currentThread().setName("AiconYardDeckingUpdateForm");

        UI.getCurrent().getPage().setTitle("Aicon Yard Decking Update");

        resultCodeField = new TextField();
        resultTextField = new TextArea();
        resultAudienceField =new TextField();
        resultLevelField = new TextField();
    }


    /**
     * Creates and returns the form layout for setting and sending connection status messages.
     *
     * @return A `Div` containing the form for sending connection status messages.
     */
    public Div getForm() {
        if (content == null) {
            // Create and configure the title
            formTitle = new H3(TITLE + " " + getUpdateLabel());
            formTitle.setWidthFull();

            Span resultCodeLabel = new Span("Result code");
            Span resultTextLabel = new Span("Result text");
            Span resultAudienceLabel = new Span("Result Audience");
            Span resultLeveLabel = new Span("Result level");

            // Create layout rows for the form fields
            HorizontalLayout row1 = new HorizontalLayout();
            row1.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            resultCodeField.setWidth(150, Unit.MM);
            TextArea textArea = new TextArea("Beschrijving");
            textArea.setValue("Meerdere regels tekst...");
            textArea.setReadOnly(true);

            row1.add(resultCodeLabel, resultCodeField);

            HorizontalLayout row3 = new HorizontalLayout();
            row3.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            resultTextField.setWidth(250, Unit.MM);
            resultTextField.setHeight(20, Unit.MM);
            resultTextField.setReadOnly(true);
            row3.add(resultTextLabel, resultTextField);

            HorizontalLayout row4 = new HorizontalLayout();
            row4.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            resultAudienceField.setWidth(150, Unit.MM);
            resultAudienceField.setReadOnly(true);
            row4.add(resultAudienceLabel, resultAudienceField);

            HorizontalLayout row5 = new HorizontalLayout();
            row5.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
            resultLevelField.setWidth(150, Unit.MM);
            resultLevelField.setReadOnly(true);
            row5.add(resultLeveLabel, resultLevelField);

            // Organize the form fields in a vertical layout
            VerticalLayout formLayout = new VerticalLayout();
            formLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
            formLayout.add(row1, row3, row4, row5);

            content = new Div();
            content.add(formTitle, formLayout);
            content.setWidthFull();
            content.setWidth(300, Unit.MM);
            content.addAttachListener(this::onAttach);
        }
        return content;
    }

    private void onAttach(AttachEvent event) {
        this.ui = event.getUI();
        LOG.info("AiconYardDeckingUpdateForm attached to UI: {}", ui);

        // Now safe to start the consumer with valid UI
        consumer.addMessageListener(this);
        startConsumer();
    }


    private void startConsumer() {
        Thread consumerThread = new Thread(this::run);
        consumerThread.setDaemon(false);
        consumerThread.start();
    }

    private void run() {
        // Initialize and start the Kafka consumer
        consumer.consumeResponseMessages();
    }

    @Override
    public void onMessageReceived(String code, String text, String audience, String level) {
        LOG.debug("Received new yard decking update response = {} - {}", code, text);

        // Use the captured UI instance if still valid
        UI currentUI = (ui != null && ui.isAttached()) ? ui : null;

        if (currentUI != null) {
            currentUI.access(() -> {
                try {

                    refreshContent(code, text, audience, level);

                    // Show a notification safely within the UI context
//                    Notification.show( code + " - " + text);
                } catch (Exception e) {
                    LOG.error("Error while updating UI: {}", e.getMessage(), e);
                }
            });
        } else {
            LOG.error("UI is detached or unavailable, skipping update.");
        }
    }

    private void refreshContent(String resultCode, String resultText, String audience, String level) {
        // Update the form's fields with the new values
        this.resultCodeField.setValue(resultCode);
        this.resultTextField.setValue(resultText);
        this.resultAudienceField.setValue(audience);
        this.resultLevelField.setValue(level);

        this.formTitle.setText(TITLE + " " + getUpdateLabel());

    }

    private String getUpdateLabel() {
        return "(Last update: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")";
    }
}
