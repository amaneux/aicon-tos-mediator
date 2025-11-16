package com.aicon.tos.connect.web.mockup;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import com.aicon.tos.connect.web.pages.MainLayout;
import com.aicon.tos.connect.web.pages.logview.LogHolder;
import com.aicon.tos.connect.web.pages.logview.VaadinLogAppender;
import com.aicon.tos.control.AiconTosControl;
import com.aicon.tos.shared.kafka.AiconTosControlConsumer;
import com.aicon.tos.shared.kafka.ProducerManager;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.atmosphere.cpr.AtmosphereFramework;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * The AiconTosControlAppListener is a ServletContextListener implementation
 * that manages the lifecycle of the AiconTosControl application. It ensures
 * proper initialization and shutdown of application components and threads.
 * <p>
 * This listener handles:
 * - Asynchronous startup of the AiconTosControl application.
 * - Graceful shutdown of application components, including Atmosphere threads
 * and Kafka consumers.
 * - Logging details of remaining active threads during shutdown.
 * <p>
 * The listener integrates with the servlet lifecycle via the contextInitialized
 * and contextDestroyed methods.
 */
public class AiconTosControlAppListener implements ServletContextListener {
    private AiconTosControl aiconTosControl;
    private static final Logger LOG = LoggerFactory.getLogger(AiconTosControlAppListener.class);
    private ThreadPoolExecutor executor;

    @Override
    public void contextInitialized(ServletContextEvent sce) {

        try {
            setUpLogAppender();

            int cpuCores = Runtime.getRuntime().availableProcessors();
            executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(cpuCores);

            // Initialize the AiconTosControl instance
            aiconTosControl = new AiconTosControl();

            if (!aiconTosControl.getConfig().hasStorageError()) {
                // Start the application in a separate thread
                startAiconTosControlAsync();
            }
        } catch (IllegalStateException e) {
            LOG.error("Failed to initialize AiconTosControl due to an illegal state.", e);
        }
    }

    private synchronized void setUpLogAppender() {
        org.slf4j.Logger logger = LoggerFactory.getLogger(MainLayout.class);

        if (LogHolder.getAppender() == null) {
            LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            ch.qos.logback.classic.Logger appLogger = loggerContext.getLogger("com.aicon"); // Attach only to com.aicon logger

            VaadinLogAppender logbackAppender = VaadinLogAppender.createLogAppender();

            // Ensure appender is not already attached
            boolean isAlreadyAttached = false;
            Iterator<Appender<ILoggingEvent>> appenderIterator = appLogger.iteratorForAppenders();
            while (appenderIterator.hasNext()) {
                Appender<ILoggingEvent> appender = appenderIterator.next();
                if ("VaadinLogAppender".equals(appender.getName())) {
                    isAlreadyAttached = true;
                    break;
                }
            }
            if (!isAlreadyAttached) {
                appLogger.addAppender(logbackAppender);
            }

            appLogger.setAdditive(false); // Prevent log propagation to the root logger
            LogHolder.setAppender(logbackAppender);

            logger.info("VaadinLogAppender successfully set up.");
        } else {
            logger.info("VaadinLogAppender was already set up.");
        }
    }

    /**
     * Starts the AiconTosControl asynchronously in a separate thread.
     */
    private void startAiconTosControlAsync() {
        new Thread(() -> {
            try {
                aiconTosControl.start();
                LOG.info("Application started successfully.");
            } catch (IllegalStateException e) {
                LOG.error("Failed to start AiconTosControl due to an illegal state.", e);
            }
        }).start();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        LOG.info("Application stopping...");
        ServletContext servletContext = sce.getServletContext();

        // Stop Kafka components
        stopKafkaComponents(servletContext);

        // Stop Atmosphere threads
        stopAtmosphereThreads(servletContext);

        // Stop AiconTosControl
        stopComponent(aiconTosControl, () -> aiconTosControl.stop());

        waitForThreadTermination();
        logRemainingThreads();

        servletContext.log("Application stopped");
        LOG.info("Application stopped");
    }

    private void stopComponent(AiconTosControl component, Runnable stopAction) {
        String successLogMessage = "AiconTosControl stopped successfully.";
        String errorLogMessage = "Error stopping AiconTosControl";
        if (component != null) {
            try {
                stopAction.run();
                LOG.info(successLogMessage);
            } catch (IllegalStateException e) {
                LOG.error("{}: Illegal state detected - {}", errorLogMessage, e.getMessage(), e);
            }
        }
    }

    private void stopKafkaComponents(ServletContext servletContext) {
        ProducerManager.closeAllProducers();

        servletContext.getAttributeNames().asIterator().forEachRemaining(attributeName -> {
            Object attribute = servletContext.getAttribute(attributeName);
            try {
                if (attribute instanceof AiconTosControlConsumer consumer) {
                    LOG.info("Stopping Kafka Consumer: {}", attributeName);
                    consumer.stop();
                }
            } catch (IllegalStateException e) {
                // Handle expected exception
                LOG.error("Error while stopping Kafka Consumer (Illegal State): {} - {}", attributeName, e.getMessage(), e);
            }
        });
    }

    private void stopAtmosphereThreads(ServletContext servletContext) {
        // Gracefully shut down the executor
        LOG.info("Initiating shutdown of Atmosphere-related threads.");
        executor.shutdown();
        try {
            if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                LOG.warn("Atmosphere threads did not terminate within the timeout, forcing shutdown...");

                // Interrupt remaining tasks and log
                executor.shutdownNow().forEach(task ->
                        LOG.info("Interrupting remaining Atmosphere task: {}", task)
                );

                // Retry termination after forced shutdown
                if (!executor.awaitTermination(2000, TimeUnit.MILLISECONDS)) {
                    LOG.error("Atmosphere-related threads failed to terminate after forced shutdown.");
                }
            } else {
                LOG.info("All Atmosphere-related threads terminated successfully.");
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for Atmosphere threads to terminate.");
            Thread.currentThread().interrupt();
        }

        // Clean up the AtmosphereFramework
        AtmosphereFramework framework = (AtmosphereFramework) servletContext.getAttribute(
                AtmosphereFramework.class.getName());
        if (framework != null) {
            LOG.info("Destroying AtmosphereFramework...");
            framework.destroy();
            servletContext.log("AtmosphereFramework destroyed.");
        }
    }

    private void waitForThreadTermination() {
        this.executor.shutdown(); // Initiates an orderly shutdown
        try {
            if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                LOG.warn("Some threads did not finish within the timeout. Forcing termination...");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            LOG.error("Interrupted while waiting for thread termination: {}", e.getMessage());
            Thread.currentThread().interrupt();
        }

        LOG.info("Executor terminated. Remaining tasks: {}", executor.getQueue().size());
    }

    private void logRemainingThreads() {
        ThreadGroup rootThreadGroup = Thread.currentThread().getThreadGroup();
        while (rootThreadGroup.getParent() != null) {
            rootThreadGroup = rootThreadGroup.getParent();
        }

        Thread[] threads = new Thread[rootThreadGroup.activeCount()];
        rootThreadGroup.enumerate(threads, true);

        for (Thread thread : threads) {
            if (thread.isAlive()) {
                LOG.error("Remaining active thread: {}", thread.getName());
            }
        }
    }
}
