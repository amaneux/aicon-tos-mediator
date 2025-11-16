package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.connect.web.pages.ViewHelper;
import com.aicon.tos.shared.kafka.AiconTosConnectSwapRequestTopicProducer;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * This class represents a form for configuring and sending AICON TOS connection status messages.
 * It includes fields for setting request details, timestamps, communication status, and CDC information.
 */
public class SwapToLoadForm {

    private final AiconTosConnectSwapRequestTopicProducer producer;

    /**
     * Constructor that initializes the producer for sending connection status messages.
     *
     * @param producer The producer used to send the connection status messages.
     */
    protected SwapToLoadForm(AiconTosConnectSwapRequestTopicProducer producer) {
        this.producer = producer;
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
        requestId.setValue("SWAP_INDEX_654321");

        // Button to send the connection status message
        Button sendMessageButton = new Button("Send", event -> {
            producer.sendAiconTosConnectRequestTopicMessage(requestId.getValue());
        });
        HorizontalLayout hl = ViewHelper.createHorizontalLayout(FlexComponent.Alignment.END);
        hl.add(requestId, sendMessageButton);

        Div content = new Div();
        content.add(formLayout, hl);

        return content;
    }
}
