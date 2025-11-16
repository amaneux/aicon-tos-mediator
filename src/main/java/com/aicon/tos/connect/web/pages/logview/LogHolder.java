package com.aicon.tos.connect.web.pages.logview;


import java.util.List;

// Utility class to hold the log appender (singleton access)
public class LogHolder {
    private static VaadinLogAppender appender;

    private LogHolder() {
        throw new IllegalStateException("Utility class");
    }

    public static void setAppender(VaadinLogAppender a) {
        appender = a;
    }

    public static VaadinLogAppender getAppender() {
        return appender;
    }

    public static List<String> getLogs() {
        return appender != null ? appender.getMessages() : List.of();
    }

    public static void clear() {
        if (appender != null) appender.clear();
    }
}