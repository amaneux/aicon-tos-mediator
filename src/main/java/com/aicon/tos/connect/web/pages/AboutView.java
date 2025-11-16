package com.aicon.tos.connect.web.pages;

import com.aicon.tos.connect.web.AppVersion;
import com.aicon.tos.connect.web.dao.AnchorInfo;
import com.avlino.common.KeyValue;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.renderer.Renderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility.Margin;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.aicon.tos.connect.web.AppVersion.VERSION_BUILD_FOR_HOST;
import static com.aicon.tos.connect.web.AppVersion.VERSION_BUILD_NUMBER;
import static com.aicon.tos.connect.web.AppVersion.VERSION_BUILD_TIME;
import static com.aicon.tos.connect.web.AppVersion.VERSION_COMMIT_AUTHOR;
import static com.aicon.tos.connect.web.AppVersion.VERSION_COMMIT_ID;
import static com.aicon.tos.connect.web.AppVersion.VERSION_COMMIT_TIME;
import static com.aicon.tos.connect.web.AppVersion.getCommitUrl;
import static com.aicon.tos.connect.web.AppVersion.getJenkinsBuildUrl;

@PageTitle("About")
@Route(value = "about", layout = MainLayout.class)
public class AboutView extends VerticalLayout {

    public static final String VERSION_NUMBER = "1.1.1";

    private static final String[][] RELEASE_NOTES   = {
            {"Groovy can be active controlled via validations element"},
            {"Improvements in logging and InterceptorView"},
            {"Position send to tos now in host format"},
            {"Added twin handling for vessel discharge"},
            {"Connector improvements and VesselDischargeAGVASC added"}
            };

    private VerticalLayout gridLayout;

    public AboutView() {
        setSpacing(false);
        setSizeFull();
        setJustifyContentMode(JustifyContentMode.CENTER);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        Image img = new Image("https://avlino.com/wp-content/uploads/2023/05/logo.svg", "Avlino Logo");
        img.setWidth("200px");
        img.getStyle().set("margin-bottom", "2rem");
        add(img);

        H1 header = new H1("Aicon ToolBox V" + VERSION_NUMBER);
        header.addClassNames(Margin.Top.MEDIUM, Margin.Bottom.SMALL);
        add(header);

        add(new Paragraph(""));
        add(new H3("The TOS-mediator is the link between the TOS and the Aicon components."));
        add(new Paragraph("Provides message flow handling for housekeeping and decking."));
        add(new Paragraph("The CDC-interceptor allows you to generate events based on specific field changes in CDC-topics."));
        add(new Paragraph(""));

        gridLayout = new VerticalLayout();
        gridLayout.setWidth("50%");
        add(gridLayout);

        // Version info grid
        String[] headers = new String[] {"Build Information", ""};
        Grid<KeyValue> versionGrid = ViewHelper.createKeyValueGrid(headers, null, true, false);
        versionGrid.setAllRowsVisible(true);
        versionGrid.setWidth(null);
        Renderer<KeyValue> linkRenderer = new ComponentRenderer<>(kv -> {
            if (kv.value() instanceof AnchorInfo anchorText) {
                return ViewHelper.createLink(anchorText, null);
            } else {
                return new Span(String.valueOf(kv.value()));
            }
        });
        versionGrid.getColumnByKey(headers[1]).setRenderer(linkRenderer);
        gridLayout.add(versionGrid);

        Properties props = AppVersion.getVersionInfo();
        List<KeyValue> versionList = new ArrayList<>();
        versionList.add(new KeyValue("Commit ID"     , AnchorInfo.class , new AnchorInfo(getCommitUrl(), props.getProperty(VERSION_COMMIT_ID))));
        versionList.add(new KeyValue("Commit Time"   , String.class     , props.getProperty(VERSION_COMMIT_TIME)));
        versionList.add(new KeyValue("Commit Author" , String.class     , props.getProperty(VERSION_COMMIT_AUTHOR)));
        versionList.add(new KeyValue("Build Number"  , AnchorInfo.class , new AnchorInfo(getJenkinsBuildUrl(), props.getProperty(VERSION_BUILD_NUMBER))));
        versionList.add(new KeyValue("Build Time"    , String.class     , props.getProperty(VERSION_BUILD_TIME)));
        versionList.add(new KeyValue("Build for host", String.class     , props.getProperty(VERSION_BUILD_FOR_HOST)));

        versionGrid.setItems(versionList);

        add(new Paragraph(""));

        // Release notes grid
        Grid<String[]> releaseGrid = ViewHelper.createStringArrayGrid(new String[] {"Release notes"}, false, false, false);
        releaseGrid.setWidthFull();
        releaseGrid.setAllRowsVisible(true);
        releaseGrid.setItems(RELEASE_NOTES);
        gridLayout.add(releaseGrid);
    }
}