package com.aicon.tos.control.mockup.views;

import com.aicon.tos.shared.AiconTosMediatorConfig;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusProducer;
import com.aicon.tos.shared.schema.ConnectionStatus;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * This class represents a form for configuring and sending AICON TOS connection status messages.
 * It includes fields for setting request details, timestamps, communication status, and CDC information.
 */
public class AiconTosConnectionStatusForm {

    private static final Logger LOG = LoggerFactory.getLogger(AiconTosConnectionStatusForm.class);
    private TextField cdcOk;
    private final AiconTosConnectionStatusProducer producer;

    /**
     * Constructor that initializes the producer for sending connection status messages.
     *
     * @param producer The producer used to send the connection status messages.
     */
    protected AiconTosConnectionStatusForm(AiconTosConnectionStatusProducer producer) {
        this.producer = producer;
    }

    /**
     * Returns the CDC Information text field.
     *
     * @return The TextField for CDC Information.
     */
    public TextField getCDCInfoField() {
        return this.cdcOk;
    }

    /**
     * Creates and returns the form layout for setting and sending connection status messages.
     *
     * @return A `Div` containing the form for sending connection status messages.
     */
    public Div getForm() {
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField requestId = new TextField("Request_id");
        requestId.setValue(AiconTosMediatorConfig.REQUEST_ID);

        TextField requestSendTimestampField = new TextField("Request_send_timestamp (sec)");
        TextField requestReceivedTimestampField = new TextField("Request_received_timestamp (sec)");
        TextField responseSendTimestampField = new TextField("Response_send_timestamp (sec)");
        TextField responseReceivedTimestampField = new TextField("Response_received_timestamp (sec)");

        // Set default timestamp values
        long requestSendTimestamp = 1000000;
        long requestReceivedTimestamp = requestSendTimestamp + 2000;
        long responseSendTimestamp = requestReceivedTimestamp + 3500;
        long responseReceivedTimestamp = responseSendTimestamp + 1000;

        requestSendTimestampField.setValue(String.valueOf(requestSendTimestamp));
        requestReceivedTimestampField.setValue(String.valueOf(requestReceivedTimestamp));
        responseSendTimestampField.setValue(String.valueOf(responseSendTimestamp));
        responseReceivedTimestampField.setValue(String.valueOf(responseReceivedTimestamp));

        // Dropdown for selecting connection status
        Select<String> connectionStatusStr = new Select<>();
        connectionStatusStr.setItems(Arrays.stream(ConnectionStatus.values())
                                             .map(ConnectionStatus::name)
                                             .toArray(String[]::new));
        connectionStatusStr.setLabel("Communication status");
        connectionStatusStr.setValue("OK");

        // Field for CDC ok
        Checkbox cdcOkField = new Checkbox("CDC Ok");
        cdcOkField.setValue(false);
        cdcOkField.addValueChangeListener(event -> {
            cdcOkField.setValue(event.getValue());
        });

        formLayout.add(requestId, requestSendTimestampField, requestReceivedTimestampField,
                       responseSendTimestampField, responseReceivedTimestampField, connectionStatusStr, cdcOkField);

        // Button to send the connection status message
        Button sendMessageButton = new Button("Send", event -> {
            producer.sendConnectionStatusMessage(
                    requestId.getValue(),
                    connectionStatusStr.getValue(),
                    cdcOkField.getValue()
            );
        });

        HorizontalLayout buttonLayout = new HorizontalLayout(sendMessageButton);
        Div content = new Div();
        content.add(formLayout, buttonLayout);

        return content;
    }
}
