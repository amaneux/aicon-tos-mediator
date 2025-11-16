package com.aicon.tos.control.mockup.views;

import com.aicon.tos.shared.kafka.AiconUserControlProducer;
import com.aicon.tos.shared.schema.UserOperatingMode;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;

/**
 * A form for user control, allowing the selection of an operating mode and sending this information using a producer.
 * It is implemented using Vaadin components to build a simple user interface.
 */
public class AiconUserControlForm {

    private static final Logger LOG = LoggerFactory.getLogger(AiconUserControlForm.class);

    /**
     * Producer for sending user control messages.
     */
    private final AiconUserControlProducer producer;

    /**
     * Constructs a AiconUserControlForm with the specified producer.
     *
     * @param producer The producer used to send user control messages.
     */
    public AiconUserControlForm(AiconUserControlProducer producer) {
        this.producer = producer;
    }

    /**
     * Creates and returns a form layout for user control. The form includes fields for ID and operating mode,
     * as well as a button to send the data using the provided producer.
     *
     * @return A Div containing the form layout with input fields and a send button.
     */
    public Div getForm() {
        FormLayout formLayout = new FormLayout();

        // TextField for entering an ID
        TextField idField = new TextField("Id");
        idField.setValue("121");

        // Select field for choosing the operating mode
        Select<String> operatingModeField = new Select<>();
        operatingModeField.setItems(Arrays.stream(UserOperatingMode.values())
                                            .map(UserOperatingMode::name)
                                            .toArray(String[]::new));
        operatingModeField.setLabel("Operating mode");
        operatingModeField.setValue("ON");

        // Select field for choosing the ignore changes (true/false)
        Select<Boolean> ignoreField = new Select<>();
        ignoreField.setItems(Boolean.TRUE, Boolean.FALSE);
        ignoreField.setLabel("AICON and TOS Ignore changes");
        ignoreField.setValue(Boolean.TRUE); // Set default value

        // Add input fields to the form layout
        formLayout.add(idField, operatingModeField, ignoreField);

        // Button to send the user control message
        Button updateButton = new Button("Send", event -> {
            producer.sendAiconUserControlMessage(idField.getValue(), operatingModeField.getValue(), ignoreField.getValue());
        });

        // Layout to hold the button
        HorizontalLayout buttonLayout = new HorizontalLayout(updateButton);

        // Create the main content div and add form layout and button layout to it
        Div content = new Div();
        content.add(formLayout, buttonLayout);

        return content;
    }
}
