package com.aicon.tos.connect.web.pages;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.flows.FlowManager;
import com.aicon.tos.connect.web.AppComposer;
import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Mediator Home")
public class HomeView extends VerticalLayout {
    private static final Logger LOG = LoggerFactory.getLogger(HomeView.class);
    private ScheduledExecutorService executor;
    private IntegerField pollIntervalField;
    private Button refreshButton;
    private int currentPollInterval = 5;
    private Div infoConfig  = new Div();
    private Div divFlowManager = new Div(FlowManager.getInstance().toString());
    private Div divInterceptor = new Div(AppComposer.getInstance().getInterceptorDecide().toString());
    private ConfigSettings config;
    private HorizontalLayout headerLayout;
    private VerticalLayout dynamiclayout = new VerticalLayout();

    /**
     * Initializes the HomeView component by setting up its layout and content.
     * This constructor configures the view with a full width layout and displays
     * information about the application configuration and FlowManager details.
     * <p>
     * Specifically, it:
     * - Sets the full width for the layout.
     * - Adds a welcome header to the view.
     * - Displays the current configuration file path using the ConfigSettings singleton instance.
     * - Displays information about FlowManager using its singleton instance.
     * - Initiates an auto-refresh mechanism to periodically update the displayed information.
     * <p>
     * This constructor relies on the configuration and flow management utilities
     * to retrieve and display the relevant details, and it ensures that the user
     * interface remains updated dynamically.
     */
    public HomeView() {
        setWidthFull();
        UI.getCurrent().setPollInterval(currentPollInterval * 1000);

        // Titel en controlpanel in een horizontale layout
        headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        headerLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        config = ConfigSettings.getInstance();

        ConfigGroup general = null;
        if (config.getStorageError() == null) {
            try {
                general = config.getMainGroup(ConfigType.General).getChildGroup(ConfigType.GeneralItems);
            } catch (Exception e) {
                LOG.error("Config item {}/{} not found.", ConfigType.General, ConfigType.GeneralItems);
            }
        }
        String title = String.format("Aicon-TOS-Mediator for %s-%s, version %s",
                general == null ? "" : general.findItem(ConfigDomain.CFG_TERMINAL_NAME).value(),
                general == null ? "" : general.findItem(ConfigDomain.CFG_ENVIRONMENT_NAME).value(),
                AboutView.VERSION_NUMBER);
        headerLayout.add(new H2(title));

        drawStaticPart();

        startAutoRefresh();
    }

    private void drawStaticPart() {
        Div textContainer = new Div();
        textContainer.setText("Refresh interval (sec):");
        textContainer.getStyle()
                .set("height", "10px")       // Vaste hoogte
                .set("min-height", "10px")   // Minimale hoogte
                .set("margin", "10px 0");    // Ruimte boven en onder

        HorizontalLayout fieldsLayout = new HorizontalLayout();
        pollIntervalField = new IntegerField();
        pollIntervalField.setWidth("60px");
        pollIntervalField.setMin(1);
        pollIntervalField.setMax(10);
        pollIntervalField.setStep(1);
        pollIntervalField.setValue(currentPollInterval);
        pollIntervalField.getStyle()
                .set("margin-bottom", "50")
                .set("margin-right", "0px")
                .set("padding", "0")
                .setTextAlign(Style.TextAlign.CENTER);

        VerticalLayout arrowButtons = new VerticalLayout();
        arrowButtons.setSpacing(false);
        arrowButtons.setPadding(false);
        arrowButtons.getStyle()
                .set("margin-left", "0px")
                .set("margin-top", "50")
                .set("height", "20px")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("justify-content", "space-between");

        Button incrementButton = new Button(new Icon(VaadinIcon.ARROW_UP));
        incrementButton.getStyle()
                .set("padding", "0")
                .set("margin", "0")
                .set("width", "10px")
                .set("height", "10px");
        incrementButton.getElement().getStyle().set("color", "black");
        incrementButton.addClickListener(event -> {
            int currentValue = pollIntervalField.getValue();
            if (currentValue < 10) {
                pollIntervalField.setValue(currentValue + 1);
                currentPollInterval = currentValue + 1;
                UI.getCurrent().setPollInterval(currentPollInterval * 1000);
            }
        });

        Button decrementButton = new Button(new Icon(VaadinIcon.ARROW_DOWN));
        decrementButton.getStyle()
                .set("padding", "0")
                .set("margin", "0")
                .set("width", "10px")
                .set("height", "10px");
        decrementButton.getElement().getStyle().set("color", "black");
        decrementButton.addClickListener(event -> {
            int currentValue = pollIntervalField.getValue();
            if (currentValue > 1) {
                pollIntervalField.setValue(currentValue - 1);
                currentPollInterval = currentValue - 1;
                UI.getCurrent().setPollInterval(currentPollInterval * 1000);
            }
        });
        arrowButtons.add(incrementButton, decrementButton);

        // Refresh
        refreshButton = new Button("Refresh Now");
        refreshButton.getStyle().set("margin-left", "0px")
                .set("margin-bottom", "50")
                .setColor("black");
        refreshButton.addClickListener(event -> refreshContent());

        fieldsLayout.setAlignItems(Alignment.BASELINE);
        fieldsLayout.add(pollIntervalField, arrowButtons, refreshButton);
        fieldsLayout.setAlignItems(Alignment.START);
        fieldsLayout.setWidth("130px");
        fieldsLayout.getStyle().set("marging", "0");

        // Control panel
        VerticalLayout controlLayout = new VerticalLayout();
        controlLayout.setSpacing(true);
        controlLayout.setWidth("280px");
        controlLayout.setAlignItems(FlexComponent.Alignment.START);
        controlLayout.add(textContainer, fieldsLayout);
        controlLayout.setHeight("50px");


        headerLayout.add(controlLayout);
        headerLayout.setSpacing(true);
        headerLayout.getStyle().set("margin-left", "0px");
        headerLayout.getStyle().set("margin-top", "0px");

        add(headerLayout);
    }

    private void drawDynamicPart() {
        remove(dynamiclayout);
        dynamiclayout.removeAll();
        dynamiclayout.add(new H3("Configuration:"));

        remove(infoConfig);
        infoConfig.removeAll();

        if (config.getStorageError() == null) {
            infoConfig.setText(String.format("    config.file = %s", ConfigSettings.getInstance().getFullFilename()));
        } else {
            Span errorSpan = new Span("(error: " + config.getStorageError().replace(",", ",\n") +
                    ") \n\nProvide configuration file and restart Mediator application");
            errorSpan.getStyle().set("color", "red");
            infoConfig.add(new Text("    config.file = "), errorSpan);
        }
        infoConfig.getStyle().set("white-space", "pre-wrap");
        dynamiclayout.add(infoConfig);
        dynamiclayout.add(new H3(""));
        dynamiclayout.add(new H3("FlowManager:"));

        remove(divFlowManager);
        divFlowManager.removeAll();
        divFlowManager = new Div(FlowManager.getInstance().toString());
        ViewHelper.preserveLineFeed(divFlowManager);
        dynamiclayout.add(divFlowManager);

        dynamiclayout.add(new H3(""));
        dynamiclayout.add(new H3(InterceptorDecide.class.getSimpleName() + ":"));

        remove(divInterceptor);
        divInterceptor.removeAll();
        divInterceptor = new Div(AppComposer.getInstance().getInterceptorDecide().toString());
        ViewHelper.preserveLineFeed(divInterceptor);
        dynamiclayout.add(divInterceptor);

        add(dynamiclayout);
    }

    /**
     * Updates the content of the provided Div components with the current configuration file path
     * and flow manager details.
     */
    private void refreshContent() {
        drawDynamicPart();
    }

    /**
     * Starts a scheduled task to periodically refresh the content displayed in the provided Div components.
     * The method creates a single-threaded scheduled executor that runs every 2 seconds, updating the content
     * of the `infoConfig` and `infoFlowManager` components within the current UI context.
     *
     * @throws IllegalStateException if the method is called without an active UI context
     */
    private void startAutoRefresh() {
        UI currentUI = UI.getCurrent();
        if (currentUI == null) {
            throw new IllegalStateException(
                    "UI.getCurrent() is null. Make sure this method is called from the UI context.");
        }
        executor = Executors.newSingleThreadScheduledExecutor();
        executor.scheduleAtFixedRate(() -> {
            currentUI.access(() -> {
                try {
                    refreshContent();
                    setViewWidth();
                } catch (Exception e) {
                    LOG.error("Error during refresh: {}", e.getMessage());
                }
            });
        }, 0, 2, TimeUnit.SECONDS);
    }

    /**
     * This method is called when the component is detached from the UI. It ensures
     * that resources such as the ScheduledExecutorService are properly released
     * by shutting it down to avoid resource leaks.
     *
     * @param detachEvent the event triggered when the component is detached
     */
    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    public void setViewWidth() {
        UI ui = UI.getCurrent();
        DataStore dataStore = DataStore.getInstance();
        if (ui != null) {
            Page page = ui.getPage();
            if (dataStore.getViewportWidth() <= 0) {
                page.executeJs("return window.innerWidth;").then(Integer.class, dataStore::setViewportWidth);
            }
            if (!Boolean.TRUE.equals(ui.getSession().getAttribute("resizeListenerAdded"))) {
                page.addBrowserWindowResizeListener(event -> {
                    dataStore.setViewportWidth(event.getWidth());
                    System.out.println("Updated Viewport Width: " + event.getWidth());
                });
                ui.getSession().setAttribute("resizeListenerAdded", true);
            }
        }
    }
}
