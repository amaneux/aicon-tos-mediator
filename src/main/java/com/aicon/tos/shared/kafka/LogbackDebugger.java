package com.aicon.tos.shared.kafka;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

/**
 * The LogbackDebugger class provides utility methods to inspect and debug the configuration of log appenders
 * in a Logback logging setup. This is especially helpful for troubleshooting or verifying logger configurations.
 */
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.slf4j.LoggerFactory;

public class LogbackDebugger {
    public static void debugLogAppenders() {
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();

        // Debug the root logger
        Logger rootLogger = context.getLogger(Logger.ROOT_LOGGER_NAME);
        System.out.println("==== Root Logger Append List ====");
        rootLogger.iteratorForAppenders().forEachRemaining(appender ->
            System.out.println("- Appender: " + appender.getName() + " (" + appender.getClass().getName() + ")")
        );

        // Debug the com.aicon logger
        Logger appLogger = context.getLogger("com.aicon");
        System.out.println("==== com.aicon Logger Append List ====");
        appLogger.iteratorForAppenders().forEachRemaining(appender ->
            System.out.println("- Appender: " + appender.getName() + " (" + appender.getClass().getName() + ")")
        );
    }
}