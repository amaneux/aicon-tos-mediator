package com.aicon.tos.shared;

/**
 * Configuration class for AiconTosMediator, containing various constants used in the application.
 * This class is designed as a utility class and cannot be instantiated.
 */
public final class AiconTosMediatorConfig {

    /**
     * A static request ID used throughout the application.
     */
    public static final String REQUEST_ID = "REQ_INDEX_444444";

    /**
     * The number of differences used in calculating averages for some control operations.
     */
    public static final int NUMBER_OF_DIFFS = 2;

    /**
     * The expected length after splitting a string in the format `key=value`.
     * This value is used to validate proper string splits in processing operations.
     */
    public static final int CORRECT_LENGTH_AFTER_SPLIT = 2;

    /**
     * Private constructor to prevent instantiation of this utility class.
     * Throws an exception if instantiation is attempted.
     */
    private AiconTosMediatorConfig() {
        throw new IllegalStateException("Cannot instantiate AiconTosMediatorConfig - utility class");
    }
}
