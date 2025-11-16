package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestProducer;
import com.aicon.tos.shared.schema.AiconYardDeckingUpdateRequestMessage;
import com.aicon.tos.shared.schema.DeckMove;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * This class represents a form for configuring and sending AICON TOS connection status messages.
 * It includes fields for setting request details, timestamps, communication status, and CDC information.
 */
public class DeckingUpdateRequestForm {

    private static final Logger LOG = LoggerFactory.getLogger(DeckingUpdateRequestForm.class);

    private static final String TITLE = "AICON_YARD_DECKING_UPDATE_REQUEST";
    private AiconYardDeckingUpdateRequestProducer producer = null;

    protected DeckingUpdateRequestForm(AiconYardDeckingUpdateRequestProducer producer) {
        this.producer = producer;

        Thread.currentThread().setName("AiconYardDeckingUpdateRequestForm");

        UI.getCurrent().getPage().setTitle("Aicon Yard Decking Update Request");
    }


    /**
     * Creates and returns the form layout for setting and sending connection status messages.
     *
     * @return A `Div` containing the form for sending connection status messages.
     */
    public Div getForm() {
        H3 formTitle;
        Div content;

        // Create and configure the title
        formTitle = new H3(TITLE + " " + getUpdateLabel());
        formTitle.setWidthFull();

        TextField requestId = new TextField("Request_id");
        requestId.setValue("cf8b606d-095f-42bc-8283-d73269cdb624");
        requestId.setWidth(150, Unit.MM);
        TextField wiGKey = new TextField("wiGkey");
        wiGKey.setValue("3437631826");
        wiGKey.setWidth(100, Unit.MM);
        TextField positionTo = new TextField("positionTo");
        positionTo.setValue("02A-098-2-5");
        positionTo.setWidth(100, Unit.MM);

        // Create layout rows for the form fields
        HorizontalLayout row1 = new HorizontalLayout();
        row1.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row1.add(requestId);

        HorizontalLayout row2 = new HorizontalLayout();
        row2.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row2.add(wiGKey);

        HorizontalLayout row3 = new HorizontalLayout();
        row3.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        row3.add(positionTo);

        HorizontalLayout row4 = new HorizontalLayout();
        // Button to send the connection status message
        Button sendMessageButton = new Button("Send", event -> {
            AiconYardDeckingUpdateRequestMessage updateRequestMessage = new AiconYardDeckingUpdateRequestMessage();
            updateRequestMessage.setRequestId(requestId.getValue());
            DeckMove deckMove = new DeckMove();
            deckMove.setPositionTo(positionTo.getValue());
            deckMove.setWiGkey(wiGKey.getValue());
            updateRequestMessage.getMoves().add(deckMove);
            producer.sendTosDeckingUpdateMessage(updateRequestMessage);
        });

        row4.add(sendMessageButton);

        // Organize the form fields in a vertical layout
        VerticalLayout formLayout = new VerticalLayout();
        formLayout.setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.START);
        formLayout.add(row1, row2, row3, row4);

        content = new Div();
        content.add(formTitle, formLayout);
        content.setWidthFull();
        content.setWidth(300, Unit.MM);

        return content;
    }

    private String getUpdateLabel() {
        return "(Last update: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) + ")";
    }
}
