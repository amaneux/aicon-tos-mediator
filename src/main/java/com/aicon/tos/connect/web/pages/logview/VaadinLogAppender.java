package com.aicon.tos.connect.web.pages.logview;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class VaadinLogAppender extends AppenderBase<ILoggingEvent> {

    private final List<String> messages = new ArrayList<>();

    @Override
    protected void append(ILoggingEvent event) {

        String timestamp = Instant.ofEpochMilli(event.getTimeStamp())
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

        String level = event.getLevel().levelStr;
        String levelColor = switch (level) {
            case "ERROR" -> "red";
            case "WARN" -> "orange";
            case "INFO" -> "green";
            case "DEBUG" -> "gray";
            default -> "black";
        };

        String htmlMessage = String.format(
                "<div style='font-family:monospace; white-space:pre-wrap;'>"
                        + "<span style='color:blue;'>%s</span> "
                        + "<span style='color:gray;'>[%s]</span> "
                        + "<span style='color:%s;'>%s</span> "
                        + "<span style='color:darkcyan;'>%s</span> - "
                        + "%s"
                        + "</div>",
                timestamp,
                event.getThreadName(),
                levelColor,
                level,
                event.getLoggerName(),
                escapeHtml(event.getFormattedMessage())
        );

        messages.add(htmlMessage);
    }

    private String escapeHtml(String input) {
        return input.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    // Synchronized access to getMessages()
    public List<String> getMessages() {
        return new ArrayList<>(messages);
    }

    public void clear() {
        synchronized (messages) {
            messages.clear();
        }
    }

    // Factory method to create the appender and set it up
    public static VaadinLogAppender createLogAppender() {
        VaadinLogAppender appender = new VaadinLogAppender();
        appender.setName("VaadinLogAppender");
        appender.start();
        return appender;
    }
}