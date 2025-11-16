package com.aicon.tos.interceptor.decide;

import com.aicon.tos.interceptor.*;
import com.aicon.tos.interceptor.decide.scenarios.Scenario;
import com.aicon.tos.shared.ResultLevel;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigType;
import com.aicon.tos.shared.config.ConfigUtil;
import com.aicon.tos.shared.util.AnsiColor;
import com.aicon.tos.shared.util.NamedThreadFactory;
import com.avlino.common.datacache.FixedSizeObservableList;
import com.avlino.common.utils.DateTimeUtils;
import generated.ConfigItemKeyEnum;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static com.avlino.common.Constants.LF;

/**
 * Manages the filtering and decision pipeline, allowing aggregated results from multiple entities, with storage.
 */
public class InterceptorDecide {

    private static final Logger LOG = LoggerFactory.getLogger(InterceptorDecide.class);

    private final InterceptorConfig config;

    private final BlockingQueue<FilteredMessage> sharedQueue = new LinkedBlockingQueue<>();
    private final ConcurrentMap<String, LinkedList<FilteredMessage>> globalMessageStorage = new ConcurrentHashMap<>();
    private final FixedSizeObservableList<MessageMeta> metaCache = new FixedSizeObservableList<>(100, FixedSizeObservableList.ListOrder.DESCENDING);
    private final Map<String, List<Scenario>> entityScenarios = new HashMap<>();

    private final ConcurrentMap<String, ExecutorService> filterExecutors = new ConcurrentHashMap<>();
    private final ExecutorService processingExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "SharedProcessingThread"));
    private final ExecutorService scenarioExecutor = Executors.newFixedThreadPool(10, new NamedThreadFactory("ScenarioProcessingThread"));

    private final List<InterceptorFilter> filters = new ArrayList<>();

    public InterceptorDecide(InterceptorConfig config) {
        this.config = config;
        initializeScenarios();
    }

    public InterceptorDecide() {
        this(new InterceptorConfig());
    }

    private void initializeScenarios() {
        if (config.getEntities() != null) {
            for (InterceptorEntityConfig entityConfig : config.getEntities()) {
                List<Scenario> scenarioList = new ArrayList<>();
                entityScenarios.put(entityConfig.getEntityName(), scenarioList);
                ConfigGroup scenarioGroup = entityConfig.getScenariosGroup();
                if (scenarioGroup != null) {
                    List<ConfigGroup> scnList = scenarioGroup.getChildren();
                    if (scnList != null) {
                        for (ConfigGroup scnConfig : scnList) {
                            if (!scnConfig.isOfType(ConfigType.Scenario)) {
                                continue;
                            }
                            String className = scnConfig.getItemValue(ConfigItemKeyEnum.CLASS_NAME.value());
                            try {
                                Scenario scenario = ConfigUtil.createObject(className, Scenario.class);
                                scenario.init(scnConfig.getName(), entityConfig);
                                scenarioList.add(scenario);
                                LOG.info("Scenario {} using class {} successful loaded, running = {}.", scnConfig.getName(), className, scenario.isRunning());
                            } catch (Exception e) {
                                LOG.error("Error initializing scenario {} using class {}, reason: {}", scnConfig.getName(), className, e.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public List<Scenario> getScenariosForEntity(String entityName) {
        return entityScenarios.get(entityName);
    }

    public void start() {
        LOG.info("Starting filtering and decision pipeline...");
        processingExecutor.submit(this::processMessages);

        config.getEntities().forEach(this::startFilterForEntity);

        LOG.info("All threads have been started.");
    }

    private void startFilterForEntity(InterceptorEntityConfig entityConfig) {
        String entityName = entityConfig.getEntityName();
        CountDownLatch latch = new CountDownLatch(1);
        InterceptorFilter filter = new InterceptorFilter(config, entityName, latch, this);
        filters.add(filter);

        ExecutorService filterExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "FilterThread-" + entityName));
        filterExecutors.put(entityName, filterExecutor);
        filterExecutor.submit(filter);

        LOG.info("Filter thread started for entityName {}", entityConfig);
    }

    private void processMessages() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                processQueueMessagesSafely();
            }
        } catch (InterruptedException e) {
            LOG.info("{} Interrupted while waiting in thread...", Thread.currentThread().getName());
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            LOG.error("Execution of processMessages() encountered an error, reason: ", e);
        } finally {
            LOG.info("Thread {} completed.", Thread.currentThread().getName());
        }
    }

    private void processQueueMessagesSafely() throws InterruptedException {
        if (sharedQueue.isEmpty()) {
            LOG.info("Queue is empty. Sleeping...");
        } else if (LOG.isDebugEnabled()) {
//            sharedQueue.forEach(message -> {
//                LOG.debug("Queued message: {}", message);
//            });
        }
        FilteredMessage message = sharedQueue.take();

        storeMessage(message);
        List<Scenario> scenarios = entityScenarios.get(message.getEntityName());
        scenarios.forEach(scenario -> {
            if (scenario.isRelevantEvent(message)) {
                if (scenario.addsMessageMeta() && !metaCache.contains(message.meta())) {
                    metaCache.add(message.meta());
                }

                scenarioExecutor.submit(() -> {
                    try {
                        scenario.processMessage(message, globalMessageStorage);
                    } catch (Exception e) {
                        if (scenario.addsMessageMeta()) {
                            message.meta().setResultWhenHigher(ResultLevel.ERROR, String.format("%s failed: %s", scenario.getName(), e.getMessage()));
                        }
                        LOG.error("{} Scenario processMessage failed due to exception, reason: {}", scenario.getName(), e.getMessage());
                    }
                    printGlobalStorage("After processing scenario (" + scenario.getName() + "): ");
                    LOG.debug("Processing time table for {}:\n{}", message.meta(), message.meta().getAllTimeStampsToString(true, DateTimeUtils.DATE_TIME_MS_FORMAT, true));
                    if (scenario.addsMessageMeta()) {
                        message.meta().addTimestamp(MessageMeta.TS_DONE, LOG);
                        message.meta().setResultWhenHigher(ResultLevel.OK, null);
                        metaCache.notifyObservers();
                    }
                });
            }
        });
    }

    private void storeMessage(FilteredMessage message) {
        String entityName = message.getEntityName();
        // printGlobalStorage("Before storing message: ");
        globalMessageStorage.computeIfAbsent(entityName, k -> new LinkedList<>()).add(message);
        // printGlobalStorage("After storing message: ");

        LOG.info("Stored message (offset={}) for entity {}.", message.getOffset(), entityName);
        enforceEntityLimits(entityName);
    }

    private void enforceEntityLimits(String entityName) {
        InterceptorEntityConfig entityConfig = config.getEntityConfig(entityName);
        LinkedList<FilteredMessage> messages = globalMessageStorage.getOrDefault(entityName, new LinkedList<>());

        synchronized (messages) {
            while (messages.size() > entityConfig.getMaxNrMessagesInStorage()
                    || exceedsMaxDuration(messages, entityConfig.getMaxTimeInStorage())) {
                LOG.info("Removing oldest message from entity {}", entityName);
                messages.removeFirst();
            }
        }
    }

    private boolean exceedsMaxDuration(LinkedList<FilteredMessage> messages, Duration maxDuration) {
        if (messages.isEmpty()) return false;
        Instant oldestTimestamp = messages.getFirst().meta().getTimestamp(MessageMeta.TS_CDC_RECEIVED);
        Instant newestTimestamp = messages.getLast().meta().getTimestamp(MessageMeta.TS_CDC_RECEIVED);
        return Duration.between(oldestTimestamp, newestTimestamp).compareTo(maxDuration) > 0;
    }

    public void addMessageToSharedQueue(FilteredMessage message) {
        try {
                sharedQueue.put(message);
                LOG.info(AnsiColor.brightYellow(
                                "Message (offset={}) added to shared queue for entity '{}', queue-size={}."),
                        message.getOffset(), message.getEntityName(), sharedQueue.size());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.error("{} got interrupted, unable to add message to shared queue.", Thread.currentThread().getName());
        }
    }

    public void shutdown() {
        LOG.info("Starting shutdown...");

        for (List<Scenario> scenarios : entityScenarios.values()) {
            for (Scenario scenario : scenarios) {
                scenario.stop();
            }
        }

        shutdownExecutor(processingExecutor);
        shutdownExecutor(scenarioExecutor);
        filterExecutors.values().forEach(this::shutdownExecutor);
        LOG.info("Shutdown complete.");
    }

    private void shutdownExecutor(ExecutorService executorService) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public List<FilteredMessage> getStoredMessages(String entityName) {
        return Collections.unmodifiableList(globalMessageStorage.getOrDefault(entityName, new LinkedList<>()));
    }

    public int getNumberOfStartedFilterThreads() {
        return filters.size();
    }

    public int getStartedProcessingThreads() {
        return 1; // Only one processing thread in this implementation
    }

    public int getStartedConsumerThreads() {
        return config.getEntityKeys().size(); // One consumer thread per entity
    }

    public void clearStorage() {
        sharedQueue.clear();
        globalMessageStorage.clear();
    }

    private void printGlobalStorage(String prefix) {
        synchronized (globalMessageStorage) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("{}: Global storage has {} entries", prefix, globalMessageStorage.size());
                return;
            }
            if (!LOG.isTraceEnabled()) {
                return;
            }

            for (Map.Entry<String, LinkedList<FilteredMessage>> entry : globalMessageStorage.entrySet()) {
                List<FilteredMessage> messages = globalMessageStorage.get(entry.getKey());
                LOG.trace("{}: Global storage messages for {}:", prefix, entry.getKey());

                if (messages != null) {
                    messages.forEach(message -> LOG.trace(message.toString()));
                } else {
                    LOG.trace("No messages stored for entityName {}", entry.getKey());
                }
            }
        }
    }

    public int getNrOfQueuedMessages() {
        return sharedQueue.size();
    }

    /**
     * @return a reference to the metadata of actual and completed filtered messages. List is in reversed order.
     */
    public List<MessageMeta> getMetaCache() {
        return metaCache;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("CDC-connectors:").append(LF);
        for (InterceptorFilter filter : filters) {
            sb.append(filter).append(LF);
        }
        sb.append("Scenarios:").append(LF);
        for (List<Scenario> scenarios : entityScenarios.values()) {
            for (Scenario scenario : scenarios) {
                sb.append(scenario).append(LF);
            }
        }
        sb.append("Filtered Message stores:").append(LF);
        for (Map.Entry<String, LinkedList<FilteredMessage>> entry : globalMessageStorage.entrySet()) {
            sb.append("  Store for ").append(entry.getKey()).append(", size = ").append(entry.getValue().size()).append(LF);
        }
        sb.append("SharedQueue       size = ").append(sharedQueue.size()).append(LF);
        sb.append("MessageMeta cache size = ").append(metaCache.size()).append(LF);
        return sb.toString();
    }
}