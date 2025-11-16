package com.aicon.tos.shared.util;

public class AnsiColor {

    // Regular Colors
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";

    // Bright Colors
    public static final String BRIGHT_BLACK = "\u001B[90m";
    public static final String BRIGHT_RED = "\u001B[91m";
    public static final String BRIGHT_GREEN = "\u001B[92m";
    public static final String BRIGHT_YELLOW = "\u001B[93m";
    public static final String BRIGHT_BLUE = "\u001B[94m";
    public static final String BRIGHT_MAGENTA = "\u001B[95m";
    public static final String BRIGHT_CYAN = "\u001B[96m";
    public static final String BRIGHT_WHITE = "\u001B[97m";

    // Background Colors
    public static final String BG_BLACK = "\u001B[40m";
    public static final String BG_RED = "\u001B[41m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_YELLOW = "\u001B[43m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_MAGENTA = "\u001B[45m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_WHITE = "\u001B[47m";

    // Bright Background Colors
    public static final String BG_BRIGHT_BLACK = "\u001B[100m";
    public static final String BG_BRIGHT_RED = "\u001B[101m";
    public static final String BG_BRIGHT_GREEN = "\u001B[102m";
    public static final String BG_BRIGHT_YELLOW = "\u001B[103m";
    public static final String BG_BRIGHT_BLUE = "\u001B[104m";
    public static final String BG_BRIGHT_MAGENTA = "\u001B[105m";
    public static final String BG_BRIGHT_CYAN = "\u001B[106m";
    public static final String BG_BRIGHT_WHITE = "\u001B[107m";

    // Reset
    public static final String RESET = "\u001B[0m";

    // Utility methods to apply colors
    public static String color(String colorCode, String message) {
        return colorCode + message + RESET;
    }

    public static String red(String message) {
        return color(RED, message);
    }

    public static String green(String message) {
        return color(GREEN, message);
    }

    public static String yellow(String message) {
        return color(YELLOW, message);
    }

    public static String blue(String message) {
        return color(BLUE, message);
    }

    public static String magenta(String message) {
        return color(MAGENTA, message);
    }

    public static String cyan(String message) {
        return color(CYAN, message);
    }

    public static String white(String message) {
        return color(WHITE, message);
    }

    public static String brightRed(String message) {
        return color(BRIGHT_RED, message);
    }

    public static String brightGreen(String message) {
        return color(BRIGHT_GREEN, message);
    }

    public static String brightYellow(String message) {
        return color(BRIGHT_YELLOW, message);
    }

    public static String brightBlue(String message) {
        return color(BRIGHT_BLUE, message);
    }

    public static String brightMagenta(String message) {
        return color(BRIGHT_MAGENTA, message);
    }

    public static String brightCyan(String message) {
        return color(BRIGHT_CYAN, message);
    }

    public static String brightWhite(String message) {
        return color(BRIGHT_WHITE, message);
    }

    public static String black(String message) {
        return color(BLACK, message);
    }

    public static String brightBlack(String message) {
        return color(BRIGHT_BLACK, message);
    }
}