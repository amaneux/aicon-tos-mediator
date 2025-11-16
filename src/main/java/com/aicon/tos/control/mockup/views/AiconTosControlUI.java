package com.aicon.tos.control.mockup.views;

import com.aicon.tos.control.mail.EmailInfoService;
import com.aicon.tos.shared.kafka.AiconTosConnectionStatusProducer;
import com.aicon.tos.shared.kafka.AiconTosControlConsumer;
import com.aicon.tos.shared.kafka.AiconUserControlProducer;
import com.aicon.tos.shared.kafka.KafkaConfig;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletService;
import jakarta.servlet.ServletContext;
import org.slf4j.LoggerFactory;

/**
 * The `AiconTosControlUI` class represents the main UI layout for the AICON TOS CONTROL environment mockup.
 * It contains tabs for different forms, allowing interaction with AICON TOS connection status,
 * user control, and CDC data. This class also manages switching between different content based on the selected tab.
 */
@PageTitle("Mediator Control")
@Route("AiconTosControl")
public class AiconTosControlUI extends VerticalLayout {

    private final AiconTosConnectionStatusProducer connectionStatusProducer;
    private final AiconUserControlProducer userControlProducer;
    private final AiconTosControlConsumer aiconTosControlConsumer;
    private final EmailInfoService emailInfoService;

    /**
     * Constructor that initializes producers, consumers, and sets up the UI components.
     */
    public AiconTosControlUI() {
        UI.getCurrent().setPollInterval(30000); // 30 seconds

        Thread.currentThread().setName("AiconTosControlUI");

        this.connectionStatusProducer = new AiconTosConnectionStatusProducer();
        this.userControlProducer = new AiconUserControlProducer();
        this.aiconTosControlConsumer = new AiconTosControlConsumer();

        // Set the consumer in the ServletContext
        addToVaadinContext("AiconTosConnectionStatusProducer", this.connectionStatusProducer);
        addToVaadinContext("AiconUserControlProducer", this.userControlProducer);
        addToVaadinContext("AiconTosControlConsumer", this.aiconTosControlConsumer);

        this.emailInfoService = new EmailInfoService();

        UI.getCurrent().addDetachListener(event -> {
            LoggerFactory.getLogger(getClass()).info("UI detached ofr AiconTosControl: {}", event.getSource());
            // Resource cleanup of specifieke acties
        });

        // Configure the UI
        this.setupUI();
    }

    private void addToVaadinContext(String name, Object kafkaComponent) {
        VaadinServletService vaadinService = (VaadinServletService) VaadinService.getCurrent();
        if (vaadinService != null) {
            ServletContext servletContext = vaadinService.getServlet().getServletContext();
            servletContext.setAttribute(name, kafkaComponent);
        }
    }


    /**
     * Sets up the user interface by creating tabs, adding content to them, and configuring their behavior.
     * This method adds different tabs for various forms in the mockup environment.
     */
    private void setupUI() {

        this.add(this.createAiconTosControlForm());

        // Create tabs
        final Tab tab1 = new Tab(KafkaConfig.AICON_TOS_CONNECTION_STATUS_TOPIC);
        final Tab tab2 = new Tab(KafkaConfig.AICON_USER_CONTROL_TOPIC);
        final Tab tab4 = new Tab("Contact");
        final Tab tab5 = new Tab("Rules editor");

        // Add tabs to the Tabs component
        final Tabs tabs = new Tabs(tab1, tab2, tab4, tab5);

        this.setTabsStyle(tabs);

        // Create content for each tab
        final Div content1 = this.createHttpSinkConnectorMockForm();
        final Div content2 = this.createUserControlMockForm();
        final Div content4 = this.createContactForm();
        final Div content5 = this.createRulesEditorForm();

        // Container to display the content
        final Div contentContainer = new Div();
        contentContainer.add(content1); // Start with the content of Tab 1
        contentContainer.setHeight(180, Unit.MM);

        // Listener to change content when a tab is selected
        tabs.addSelectedChangeListener(event -> {
            contentContainer.removeAll(); // Remove current content
            final Tab selectedTab = tabs.getSelectedTab();

            // Display content based on the selected tab
            if (selectedTab.equals(tab1)) {
                contentContainer.add(content1);
            } else {
                if (selectedTab.equals(tab2)) {
                    contentContainer.add(content2);
                } else {
                    if (selectedTab.equals(tab4)) {
                        contentContainer.add(content4);
                    } else {
                        if (selectedTab.equals(tab5)) {
                            contentContainer.add(content5);
                        }
                    }
                }
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
     * Creates the form for the HTTP sink connector mock.
     *
     * @return A `Div` containing the form.
     */
    private Div createHttpSinkConnectorMockForm() {
        AiconTosConnectionStatusForm aiconTosConnectionStatusForm = new AiconTosConnectionStatusForm(
                this.connectionStatusProducer);
        return aiconTosConnectionStatusForm.getForm();
    }

    /**
     * Creates the user control form.
     *
     * @return A `Div` containing the user control form.
     */
    private Div createUserControlMockForm() {
        AiconUserControlForm aiconUserControlForm = new AiconUserControlForm(this.userControlProducer);
        return aiconUserControlForm.getForm();
    }

    /**
     * Creates the AICON TOS control form.
     *
     * @return A `Div` containing the AICON TOS control form.
     */
    private Div createAiconTosControlForm() {
        AiconTosControlForm aiconTosControlForm = new AiconTosControlForm(this.aiconTosControlConsumer);
        return aiconTosControlForm.getForm();
    }

    /**
     * Creates the Contact form.
     *
     * @return A `Div` containing the Contact form.
     */
    private Div createContactForm() {
        ContactForm contactForm = new ContactForm(emailInfoService);
        return contactForm.getForm();
    }

    /**
     * Creates the Contact form.
     *
     * @return A `Div` containing the Contact form.
     */
    private Div createRulesEditorForm() {
        RuleEditorForm ruleEditorForm = new RuleEditorForm();
        return ruleEditorForm.getForm();
    }
}
