package com.aicon.tos.interceptor;

import com.aicon.tos.interceptor.decide.InterceptorDecide;
import com.aicon.tos.interceptor.decide.scenarios.Scenario;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * Represents a filter for processing messages from a specific entity/topic.
 * Filters data and finds changes in messages for further processing.
 */
public class InterceptorFilter implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorFilter.class);

    private final InterceptorConfig config;
    private final String entityName;
    private final String topicName;
    private final CountDownLatch latch;
    private final InterceptorDecide decide;

    private volatile boolean running = true;
    private Thread runThread;
    private Future<?> collectTask;
    private CollectedMessage lastProcessedMessage;
    private InterceptorCollect collector = null;

    /**
     * Constructs the InterceptorFilter for a specific topic.
     *
     * @param config The configuration details for this filter.
     * @param entityName The entity name.
     * @param latch  Synchronization mechanism for shutting down filters.
     * @param decide The decide logic to forward filtered messages.
     */
    public InterceptorFilter(InterceptorConfig config, String entityName,  CountDownLatch latch, InterceptorDecide decide) {
        this.config = config;
        this.entityName = entityName;
        this.latch = latch;
        this.decide = decide;

        this.topicName = config.getEntityConfig(entityName).getTopicName();
        LOG.info("Started for entity {}", this.entityName);
    }

    public String toString() {
        return String.format("  Filter on entity=%s, topic=%s, %s", entityName, topicName, collector != null ? collector.getConnectorStatus() : null);
    }

    @Override
    public void run() {
        runThread = Thread.currentThread();
        runThread.setName("FilterThread-" + entityName);

        collector = new InterceptorCollect(config.getEntityConfig(entityName));

        while (running && !Thread.currentThread().isInterrupted()) {
            List<CollectedMessage> messages = collector.collectMessages();
            LOG.trace("Collected {} messages for topic {}", messages.size(), topicName);
            if (messages != null && !messages.isEmpty()) {
                messages.forEach(this::sendMessageToDecideIfNeeded);
            }
            LOG.trace("Message in sharedQueue: {}", decide.getNrOfQueuedMessages());
        }

        if (collectTask != null) {
            try {
                collectTask.get();
            } catch (InterruptedException e) {
                LOG.info("Interrupted while waiting in thread...");
                Thread.currentThread().interrupt();
            } catch (ExecutionException e) {
                LOG.error("Execution encountered an error", e);
            }
        }

        shutdown();
        LOG.info("Thread completed for topic: {}", entityName);
    }

    /**
     * Stops the filter processing loop.
     */
    public void stopRunning() {
        running = false;
        if (runThread != null) {
            runThread.interrupt();
        }
    }

    /**
     * Checks if collected message is relevant for decide logic to process.
     *
     * @param message The collected message to evaluate for further processing or not.
     */
    void sendMessageToDecideIfNeeded(CollectedMessage message) {
        this.lastProcessedMessage = message;

        boolean relevant = false;
        for (Scenario scenario: decide.getScenariosForEntity(message.getEntityName())) {
            if (scenario.isRelevantEvent(message)) {
                relevant = true;    // happy at first match
                break;
            }
        }
        if (relevant) {
            decide.addMessageToSharedQueue(new FilteredMessage(message));
            LOG.info("Message is relevant and added to decide queue: {}", message);
        } else if (LOG.isTraceEnabled()) {
            LOG.trace("Message not relevant for decide: {}", message);
        }

        latch.countDown();
    }

    /**
     * Sets the future task for this filter.
     *
     * @param future The future of the collecting task.
     */
    public void setFuture(Future<?> future) {
        this.collectTask = future;
    }

    /**
     * Gets the last processed message.
     *
     * @return The last collected and processed message.
     */
    public CollectedMessage getLastProcessedMessage() {
        return lastProcessedMessage;
    }

    /**
     * Shuts down the filter by stopping the consumer and interrupting tasks.
     */
    public void shutdown() {
        running = false;

        if (runThread != null) {
            runThread.interrupt();
        }

        if (collectTask != null && !collectTask.isDone()) {
            collectTask.cancel(true);
        }

        LOG.info("Shutdown complete for topic {}", entityName);
    }
}