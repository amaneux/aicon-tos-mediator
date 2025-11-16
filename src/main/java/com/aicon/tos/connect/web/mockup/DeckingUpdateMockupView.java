package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.connect.web.pages.MainLayout;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateRequestProducer;
import com.aicon.tos.shared.kafka.AiconYardDeckingUpdateResponseConsumer;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.logging.Logger;

/**
 * The `TosDeckingUpdateMockupView` class represents the main UI layout for the AICON TOS CONTROL environment mockup.
 * It contains tabs for different forms, allowing interaction with AICON TOS connection status,
 * user control, and CDC data. This class also manages switching between different content based on the selected tab.
 */
@PageTitle("Mediator Decking Update")
@Route(value = "TosDeckingUpdate", layout = MainLayout.class)
public class DeckingUpdateMockupView extends VerticalLayout {

    private static final Logger LOG = Logger.getLogger(DeckingUpdateMockupView.class.getName());

    private AiconYardDeckingUpdateRequestProducer requestProducer = null;
    private AiconYardDeckingUpdateResponseConsumer responseConsumer = null;

    private DeckingUpdateResponseForm deckingUpdateResponseForm;
    private String connError = null;
    /**
     * Constructor that initializes producers, consumers, and sets up the UI components.
     */
    public DeckingUpdateMockupView() {
        LOG.info("Constructor");

        try {
            this.requestProducer = new AiconYardDeckingUpdateRequestProducer();
        } catch (Exception e) {
            LOG.severe("Failed to initialize producer, reason: " + e);
            connError = "Error while creating producer! Is the connection OK? When fixed return to this page.";
        }

        try {
            this.responseConsumer = new AiconYardDeckingUpdateResponseConsumer();
        } catch (Exception e) {
            LOG.severe("Failed to initialize consumer, reason: " + e);
            connError = "Error while creating consumer! Is the connection OK? When fixed return to this page.";
        }

        // Configure the UI
        this.setupUI();
    }

    /**
     * Sets up the user interface by creating tabs, adding content to them, and configuring their behavior.
     * This method adds different tabs for various forms in the mockup environment.
     */
    private void setupUI() {
        this.add(this.createDeckingUpdateRequestForm());
        this.add(new Hr());
        deckingUpdateResponseForm = new DeckingUpdateResponseForm(responseConsumer);
        add(deckingUpdateResponseForm.getForm());
    }

    private Div createDeckingUpdateRequestForm() {
        DeckingUpdateRequestForm deckingUpdateRequestForm = new DeckingUpdateRequestForm(requestProducer);
        return deckingUpdateRequestForm.getForm();
    }

}
