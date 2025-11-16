package com.aicon.tos.connect.web.pages;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.web.mockup.AiconTosConnectMockupView;
import com.aicon.tos.connect.web.mockup.DeckingUpdateMockupView;
import com.aicon.tos.connect.web.pages.logview.LogHolder;
import com.aicon.tos.connect.web.pages.logview.VaadinLogAppender;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Footer;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Header;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * The main view is a top-level placeholder for other views.
 */
@CssImport(
        themeFor = "vaadin-grid",
        value = "./aicon-styles.css"
)
public class MainLayout extends AppLayout {

    private H3 viewTitle;

    public MainLayout() {

//        setUpLogAppender();

        setPrimarySection(Section.DRAWER);
        addDrawerContent();
        addHeaderContent();

        Div contentContainer = new Div();
        contentContainer.setSizeFull();
        addToNavbar(contentContainer);

        UI.getCurrent().navigate(HomeView.class);
    }



    private void addHeaderContent() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setAriaLabel("Menu toggle");

        viewTitle = new H3();
        viewTitle.setWidth("25em");
        viewTitle.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Margin.NONE);

        addToNavbar(true, toggle, viewTitle);
    }

    private void addDrawerContent() {
        H3 appName = new H3("TOS Mediator");
        appName.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.Margin.NONE);
        Header header = new Header(appName);
        VerticalLayout hl = ViewHelper.createVerticalLayout();
        hl.add(header);

        Scroller scroller = new Scroller(createNavigation());

        addToDrawer(hl, scroller, createFooter());
    }

    private SideNav createNavigation() {
        SideNav nav = new SideNav();


        nav.addItem(new SideNavItem("Home", HomeView.class));
        SideNavItem configItem = new SideNavItem("Configuration", ConfigurationView.class);
        nav.addItem(configItem);
        configItem.addItem(new SideNavItem("Down/Upload", ConfigUploadView.class));
        nav.addItem(new SideNavItem("Mockup tester", AiconTosConnectMockupView.class));
        nav.addItem(itemEnabledDependingOnCanary(new SideNavItem("Canary History", CanarySessionsView.class)));
        nav.addItem(itemEnabledDependingOnCanary(new SideNavItem("CDC Information", CDCInfoView.class)));
        nav.addItem(new SideNavItem("Replay CDC-scenario", CDCReplayView.class));
        nav.addItem(new SideNavItem("Tos Decking Update", DeckingUpdateMockupView.class));
        nav.addItem(new SideNavItem("Tos Canary Mockup", TOSCanaryMockupView.class));
        nav.addItem(new SideNavItem("About", AboutView.class));
//        nav.addItem(new SideNavItem("DetailsGrid", DetailsGridView.class));       keep for testing purpose

        nav.getElement().appendChild(createExternalLink("Control", "aiconTosControl-view"));
        nav.getElement().appendChild(createExternalLink("Interceptor", "interceptor-view"));

        nav.getElement().appendChild(createExternalLink("Logging", "log-viewer"));

        return nav;
    }

    private SideNavItem itemEnabledDependingOnCanary(SideNavItem item) {

        ConfigSettings configSettings = ConfigSettings.getInstance();
        if (configSettings.hasStorageError()) {
            item.setEnabled(false);
        } else {
            ConfigGroup tosControlConfig = configSettings.getMainGroup(ConfigType.TosControl);
            ConfigGroup canaryConfig = tosControlConfig.getChildGroup(ConfigType.CanaryCheck);
            boolean canaryIsEnabled = canaryConfig != null &&
                    (canaryConfig.getItemValue(ConfigDomain.CFG_CANARY_ON_OFF) == null ||
                    !canaryConfig.getItemValue(ConfigDomain.CFG_CANARY_ON_OFF).equalsIgnoreCase("off"));
            item.setEnabled(canaryIsEnabled);
            if (!canaryIsEnabled) {
                item.getElement().setAttribute("title", "Canary is disabled in the configuration.");
            }
        }
        return item;
    }

    private Element createExternalLink(String title, String url) {
        Anchor anchor = new Anchor(url, title);
        anchor.setTarget("_blank");

        anchor.getStyle()
                .set("display", "block")
                .set("padding", "0.5em 1em")
                .set("text-align", "center")
                .set("border", "1px solid var(--lumo-primary-color)")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("background-color", "white")
                .set("color", "var(--lumo-primary-color)")
                .set("text-decoration", "none")
                .set("font-weight", "500")
                .set("box-shadow", "var(--lumo-box-shadow-xs)")
                .set("margin", "var(--lumo-space-xs) 0");

        Element customItem = new Element("vaadin-side-nav-item");
        customItem.appendChild(anchor.getElement());
        return customItem;
    }


    private Footer createFooter() {
        return new Footer();
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        updateTitle();
    }

    private void updateTitle() {
        viewTitle.setText(getCurrentPageTitle());
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
