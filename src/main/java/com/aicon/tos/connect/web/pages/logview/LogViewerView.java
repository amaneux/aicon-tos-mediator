package com.aicon.tos.connect.web.pages.logview;

import com.vaadin.flow.component.UIDetachedException;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@PageTitle("Mediator Logging")
@Route("log-viewer")
public class LogViewerView extends VerticalLayout {

    private final Div logArea = new Div();

    private final Button refreshButton = new Button("Refresh", e -> refreshLogs());
    private final Button clearButton = new Button("Clear", e -> {
        LogHolder.clear();
        refreshLogs();
    });

    private final Checkbox autoRefreshCheckbox = new Checkbox("Auto Refresh", true);
    private final NumberField fontSizeField = new NumberField("Font size");

    private final TextField keyword1 = new TextField("Filter 1");
    private final TextField keyword2 = new TextField("Filter 2");
    private final TextField keyword3 = new TextField("Filter 3");

    private final RadioButtonGroup<String> filterMode = new RadioButtonGroup<>();
    private final NumberField linesBefore = new NumberField("Lines before");
    private final NumberField linesAfter = new NumberField("Lines after");

    private final transient  ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public LogViewerView() {
        // Font size field
        fontSizeField.setValue(14.0);
        fontSizeField.setMin(10);
        fontSizeField.setMax(30);
        fontSizeField.setStep(1);
        fontSizeField.setWidth("120px");
        fontSizeField.addValueChangeListener(e ->
                logArea.getStyle().set("font-size", e.getValue().intValue() + "px"));

        // Filter mode
        filterMode.setLabel("Filter mode");
        filterMode.setItems("AND", "OR");
        filterMode.setValue("AND");

        // Filter fields with clear buttons and refresh on change
        keyword1.setClearButtonVisible(true);
        keyword2.setClearButtonVisible(true);
        keyword3.setClearButtonVisible(true);

        keyword1.setValueChangeMode(ValueChangeMode.EAGER);
        keyword2.setValueChangeMode(ValueChangeMode.EAGER);
        keyword3.setValueChangeMode(ValueChangeMode.EAGER);

        // Context lines
        linesBefore.setValue(0.0);
        linesBefore.setMin(0);
        linesBefore.setMax(100);
        linesBefore.setStep(1);
        linesBefore.setWidth("100px");
        linesBefore.setAllowedCharPattern("0123456789");
        linesBefore.setPlaceholder("0");
        linesBefore.addValueChangeListener(e -> {
            if (e.getValue() == null) {
                linesBefore.setValue(0.0);
            }
        });

        linesAfter.setValue(0.0);
        linesAfter.setMin(0);
        linesAfter.setMax(100);
        linesAfter.setStep(1);
        linesAfter.setWidth("100px");
        linesAfter.setAllowedCharPattern("0123456789");
        linesAfter.setPlaceholder("0");
        linesAfter.addValueChangeListener(e -> {
            if (e.getValue() == null) {
                linesBefore.setValue(0.0);
            }
        });

        // Style log area
        logArea.getStyle()
                .set("background-color", "#fafafa")
                .set("overflow", "auto")
                .set("border", "1px solid #ccc")
                .set("padding", "10px")
                .set("resize", "both")
                .set("min-height", "400px")
                .set("min-width", "900px")
                .set("max-height", "800px")
                .set("font-size", fontSizeField.getValue().intValue() + "px");

        // Controls and layout
        HorizontalLayout controls = new HorizontalLayout(
                refreshButton, clearButton, autoRefreshCheckbox, fontSizeField
        );
        controls.setWidthFull();
        controls.setDefaultVerticalComponentAlignment(Alignment.END);

        HorizontalLayout filters = new HorizontalLayout(
                keyword1, keyword2, keyword3, filterMode, linesBefore, linesAfter
        );
        filters.setWidthFull();
        filters.setDefaultVerticalComponentAlignment(Alignment.END);

        add(logArea, controls, filters);
        setPadding(true);

        refreshLogs();
        startAutoRefresh();
    }

    private void refreshLogs() {
        getUI().ifPresent(ui -> ui.access(() -> {
            logArea.removeAll();

            List<String> logs = LogHolder.getLogs();
            int before = getLinesBefore();
            int after = getLinesAfter();
            boolean useAnd = "AND".equals(filterMode.getValue());
            String[] keywords = getKeywords();

            boolean[] matchMap = buildMatchMap(logs, keywords, useAnd);

            renderLogs(logs, matchMap, before, after, keywords);

            scrollToBottom();
        }));
    }

    private int getLinesBefore() {
        return linesBefore.getValue() != null ? linesBefore.getValue().intValue() : 0;
    }

    private int getLinesAfter() {
        return linesAfter.getValue() != null ? linesAfter.getValue().intValue() : 0;
    }

    private String[] getKeywords() {
        return new String[]{
                keyword1.getValue().trim().toLowerCase(),
                keyword2.getValue().trim().toLowerCase(),
                keyword3.getValue().trim().toLowerCase()
        };
    }

    private boolean[] buildMatchMap(List<String> logs, String[] keywords, boolean useAnd) {
        boolean[] matchMap = new boolean[logs.size()];

        for (int i = 0; i < logs.size(); i++) {
            String plainText = logs.get(i).replaceAll("<[^>]*>", "").toLowerCase();

            if (useAnd) {
                matchMap[i] = allKeywordsMatch(plainText, keywords);
            } else {
                matchMap[i] = anyKeywordMatches(plainText, keywords);
            }
        }

        return matchMap;
    }

    private boolean allKeywordsMatch(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && !text.contains(keyword)) {
                return false;
            }
        }
        return true;
    }

    private boolean anyKeywordMatches(String text, String[] keywords) {
        for (String keyword : keywords) {
            if (!keyword.isEmpty() && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }


    private void renderLogs(List<String> logs, boolean[] matchMap, int before, int after, String[] keywords) {
        for (int i = 0; i < logs.size(); i++) {
            if (isInContext(matchMap, i, before, after)) {
                String html = logs.get(i);
                if (matchMap[i]) {
                    html = highlightKeywords(html, keywords);
                }
                addLogToView(html);
            }
        }
    }

    private boolean isInContext(boolean[] matchMap, int currentIndex, int before, int after) {
        for (int j = Math.max(0, currentIndex - before); j <= Math.min(matchMap.length - 1, currentIndex + after); j++) {
            if (matchMap[j]) {
                return true;
            }
        }
        return false;
    }

    private void addLogToView(String html) {
        Div logLine = new Div();
        logLine.getElement().setProperty("innerHTML", html);
        logArea.add(logLine);
    }

    private void scrollToBottom() {
        logArea.getElement().executeJs("this.scrollTop = this.scrollHeight");
    }
    private String highlightKeywords(String html, String[] keywords) {
        String[] colors = {
                "#ffff99", // yellow
                "#ffc0c0", // red
                "#ccf2ff"  // blue
        };

        String result = html;

        for (int i = 0; i < keywords.length; i++) {
            String keyword = keywords[i];
            if (keyword == null || keyword.isEmpty()) continue;

            String color = colors[i % colors.length];

            // Replace matches case-insensitively, preserve original text
            result = result.replaceAll("(?i)(" + Pattern.quote(keyword) + ")",
                    "<span style='background-color:" + color + ";'>$1</span>");
        }

        return result;
    }


    private void startAutoRefresh() {
        VaadinSession session = VaadinSession.getCurrent();
        scheduler.scheduleAtFixedRate(() -> {
            // Ensure the session and UI are still valid
            if (session != null && !session.hasLock() && autoRefreshCheckbox.getValue()) {
                try {
                    session.access(() -> {
                        if (!refreshLogsSafely()) {
                            scheduler.shutdown(); // Stop refreshing if the session/UI is invalid
                        }
                    });
                } catch (UIDetachedException ex) {
                    // Handle exceptions silently and shutdown scheduler to avoid unnecessary executions
                    scheduler.shutdown();
                }
            }
        }, 1, 5, TimeUnit.SECONDS);
    }

    private boolean refreshLogsSafely() {
        return getUI().map(ui -> {
            try {
                refreshLogs(); // Refresh logs, ensure UI validation is done before calling this
                return true;
            } catch (UIDetachedException ex) {
                return false;
            }
        }).orElse(false);
    }
}
