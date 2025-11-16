package com.aicon.tos.connect.web.mockup;

import com.aicon.tos.connect.web.pages.MainLayout;
import com.aicon.tos.shared.kafka.AiconTosConnectSwapRequestTopicProducer;
import com.aicon.tos.shared.kafka.AiconTosConnectWQRequestTopicProducer;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.logging.Logger;

/**
 * The `AiconTosControlMockupView` class represents the main UI layout for the AICON TOS CONTROL environment mockup.
 * It contains tabs for different forms, allowing interaction with AICON TOS connection status,
 * user control, and CDC data. This class also manages switching between different content based on the selected tab.
 */
@PageTitle("Mediator Mockup tester")
@Route(value = "AiconTosConnect", layout = MainLayout.class)
public class AiconTosConnectMockupView extends VerticalLayout {

    private static final Logger LOG = Logger.getLogger(AiconTosConnectMockupView.class.getName());

    private String connError = null;


    private AiconTosConnectSwapRequestTopicProducer swapRequestProducer = null;
    private AiconTosConnectWQRequestTopicProducer wqRequestProducer = null;

    /**
     * Constructor that initializes producers, consumers, and sets up the UI components.
     */
    public AiconTosConnectMockupView() {
        LOG.info("Constructor");

        try {
            this.swapRequestProducer = new AiconTosConnectSwapRequestTopicProducer();
            this.wqRequestProducer = new AiconTosConnectWQRequestTopicProducer();
        } catch (Exception e) {
            LOG.severe("Failed to initialize producers, reason: " + e);
            connError = "Error while creating producers! Is the connection OK? When fixed return to this page.";
        }

        // Configure the UI
        this.setupUI();
    }

    /**
     * Sets up the user interface by creating tabs, adding content to them, and configuring their behavior.
     * This method adds different tabs for various forms in the mockup environment.
     */
    private void setupUI() {
        // Create tabs
        final Tab tab1 = new Tab("aicon_dispatch_itv_job_resequence_request");
        final Tab tab2 = new Tab("aicon_dispatch_work_queue_activation_request");

        // Add tabs to the Tabs component
        final Tabs tabs = new Tabs(tab1, tab2);

        this.setTabsStyle(tabs);

        // Create content for each tab
        final Div error = new Div(connError);
        final Div content1 = this.createSwapToLoadForm();
        final Div content2 = this.createActivateWorkQueueForm();

        // Container to display the content
        final Div contentContainer = new Div();
        contentContainer.add(error, content1); // Start with the content of Tab 1
        contentContainer.setHeight(180, Unit.MM);

        // Listener to change content when a tab is selected
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll(); // Remove current content
            final Tab selectedTab = tabs.getSelectedTab();

            // Display content based on the selected tab
            if (selectedTab.equals(tab1)) {
                contentContainer.add(content1);
            } else {
                contentContainer.add(content2);
            }

            this.setTabsStyle(tabs);
        });

        this.add(tabs, contentContainer);
    }

    /**
     * Sets the style for the tabs, highlighting the selected tab.
     *
     * @param tabs The `Tabs` component containing the tabs to style.
     */
    private void setTabsStyle(Tabs tabs) {
        tabs.getChildren().forEach(tab -> {
            tab.getElement().getStyle().remove("border");
            tab.getElement().getStyle().remove("color"); // Reset text color
        });

        Tab selectedTab = tabs.getSelectedTab();

        selectedTab.getElement().getStyle().set("border", "1px solid grey");
        selectedTab.getElement().getStyle().set("border-radius", "4px");
        selectedTab.getElement().getStyle().set("color", "green");
    }


    /**
     * Creates the swapToLoad request.
     *
     * @return A `Div` containing the AICON TOS control form.
     */
    private Div createSwapToLoadForm() {
        SwapToLoadForm aiconTosControlForm = new SwapToLoadForm(
                swapRequestProducer);
        return aiconTosControlForm.getForm();
    }


    /**
     * Creates the activateWorkQueue form.
     *
     * @return A `Div` containing the AICON TOS control form.
     */
    private Div createActivateWorkQueueForm() {
        ActivateWorkQueueForm aiconTosControlForm = new ActivateWorkQueueForm(
                wqRequestProducer);
        return aiconTosControlForm.getForm();
    }
}
