package com.aicon.tos.interceptor.newgenproducerconsumer.testcode;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.kafka.KafkaConfig;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.config.ConfigException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Provides admin services for kafka like a cache for all available topics, connectivity questions, etc.
 */
public class KafkaAdmin {
    private Logger LOG = LoggerFactory.getLogger(KafkaAdmin.class);

    private static final Object SYNC_LOCK = new Object();
    private static KafkaAdmin _instance;

    private List<String> sortedTopics;
    private AdminClient adminClient = null;
    private long lastConnCheckTime = 0;
    private boolean connected = false;

    public static KafkaAdmin getInstance() {
        return getInstance(false);
    }

    public static KafkaAdmin getInstance(boolean useDefaults) {
        if (_instance == null) {
            synchronized (SYNC_LOCK) {
                _instance = new KafkaAdmin();
                _instance.getKafkaAdminClient();
            }
        }
        return _instance;
    }

    private KafkaAdmin() {}

    public AdminClient getKafkaAdminClient() {
        if (adminClient != null) {
            return adminClient;
        }
        try {
            Properties props = KafkaConfig.getConsumerProps();
            adminClient = AdminClient.create(props);
        } catch (Exception e) {
            LOG.error("Could not create Kafka AdminClient, reason: {}", e.getMessage());
        }
        return adminClient;
    }

    /**
     * This method provides a lightweight verification if the kafka server is reachable or not (within 1000 ms).
     * Kafka only checks connectivity when really needed, so when using poll(), listTopics(), etc.
     * Note: this method will check connectivity when last attempt was > 5000 ms ago (in case of multiple consumers
     *       asking the same almost in parallel).
     * @return true when kafka server is reachable
     */
    public boolean isKafkaReachable() {
        if (getKafkaAdminClient() == null) {
            return false;
        }
        synchronized (SYNC_LOCK) {
            try {
                if (System.currentTimeMillis() - lastConnCheckTime > 5000) {
                    lastConnCheckTime = System.currentTimeMillis();
                    getKafkaAdminClient().describeCluster().nodes().get(1000, TimeUnit.MILLISECONDS);
                    connected = true;
                }
            } catch (ExecutionException | InterruptedException | TimeoutException e) {
                String host = KafkaConfig.getConsumerProps().getProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG);
                LOG.error("Could not connect to Kafka broker {}, reason: {}", host, e.getMessage());
                connected = false;
            }
        }
        return connected;
    }

    public List<String> getAllTopicsSorted() {
        if (getKafkaAdminClient() == null) {
            return null;
        }
        if (sortedTopics != null) {
            return sortedTopics;
        }
        try {
            Set<String> topics = adminClient.listTopics(new ListTopicsOptions().timeoutMs(5000)).names().get();
            sortedTopics = new ArrayList<>(topics);
            sortedTopics.sort(String::compareTo);
        } catch (ExecutionException | InterruptedException e) {
            LOG.error("Could not fetch topics from Kafka, reason: {}", e.getMessage());
        }
        return sortedTopics;
    }

    public boolean topicExistsExactly(String topic) {
        return getAllTopicsSorted() != null ? getAllTopicsSorted().contains(topic) : false;
    }

    public boolean prefixedTopicExistsExactly(String topic) {
        return getAllTopicsSorted() != null ? getAllTopicsSorted().contains(ConfigDomain.prefixIfNeeded(topic)) : false;
    }

    public boolean topicExistsPartly(String topic) {
        return getAllTopicsSorted() != null ? getAllTopicsSorted().stream().anyMatch(t -> t.contains(topic)) : false;
    }

    public static void main(String[] args) {
        KafkaAdmin kafkaAdmin = KafkaAdmin.getInstance(true);

        System.out.println("Topic inv_wi exactly exists: " + kafkaAdmin.topicExistsExactly("inv_wi"));

        System.out.println("Topic inv_wi partly exists: " + kafkaAdmin.topicExistsPartly("inv_"));

        System.out.println("Kafka topics: " + kafkaAdmin.sortedTopics);
    }
}