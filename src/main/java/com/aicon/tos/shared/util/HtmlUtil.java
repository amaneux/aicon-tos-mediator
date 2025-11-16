package com.aicon.tos.shared.util;

public class HtmlUtil {

    /**
     * Escapes special HTML characters so the string can be safely used in innerHTML.
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
