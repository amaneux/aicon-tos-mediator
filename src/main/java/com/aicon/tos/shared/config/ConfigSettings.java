package com.aicon.tos.shared.config;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.connect.web.AppComposer;
import com.aicon.tos.shared.util.AnsiColor;
import com.avlino.common.datasources.MongoReader;
import com.avlino.common.utils.FileUtils;
import com.avlino.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Singleton class to allow access to the configuration from any object in the application.
 * todo ron a faulty config should never lead to the webpage not showing up (but now it does).
 * todo ron we must be able to only run  with partly defined config, so for example only running interceptor without canary (or reverse). Now it is to much entangled
 */
public class ConfigSettings {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigSettings.class);

    public static final String AICON_ENV_PROPS_FILENAME = "aicon-connections.xml";
    static final String AICON_ENV_XSD_FILENAME          = "connect-configuration.xsd";
    static final String FOLDER_UP                       = "..";
    static final String AICON_ENV_PROPS_CONF_PATH       = FOLDER_UP + "/conf/mediator/";        // in classpath
    static final String AICON_ENV_XSD_PATH              = AICON_ENV_PROPS_CONF_PATH + "schema"; // in classpath

    public static final String CFG_ENVIRONMENT_NAME = "environment.name";
    public static final String CFG_TERMINAL_NAME = "terminal.name";
    public static final String CFG_YARD_SCOPE = "yard.scope";
    public static final String CFG_CANARY_INTERVAL_MS = "canary.interval.ms";
    public static final String CFG_CANARY_CDC_FREQUENCY = "canary.cdc.frequency";
    public static final String CFG_CANARY_ON_OFF = "canary.on.off";
    public static final String CFG_CDC_PREFIX = "cdc.topic.prefix";
    public static final String CFG_CONNECTION_TIMEOUT_MS = "connection.timeout.ms";

    public static final String CFG_EMAIL_HOSTNAME = "email.hostname";
    public static final String CFG_EMAIL_SMTPPORT = "email.smtpport";
    public static final String CFG_EMAIL_USERNAME = "email.username";
    public static final String CFG_EMAIL_PASSWORD = "email.password";
    public static final String CFG_EMAIL_SENDER = "email.sender";

    public static final String CFG_HTTP_URL = "http.url";
    public static final String CFG_HTTP_METHOD = "http.method";
    public static final String CFG_DEFAULT_METHOD = "POST";
    public static final String CFG_HTTP_CONNECT_TIMEOUT_MS = "http.connect.timeout.ms";
    public static final String CFG_HTTP_READ_TIMEOUT_MS = "http.read.timeout.ms";
    public static final String CFG_HTTP_CONTENT_TYPE = "http.content.type";
    public static final String CFG_HTTP_SOAP_ACTION = "http.soap.action";
    public static final String CFG_HTTP_USERNAME = "http.username";
    public static final String CFG_HTTP_PASSWORD = "http.password";

    public static final String CFG_KAFKA_HOST = "hostname";
    public static final String CFG_KAFKA_PORT = "hostport";
    public static final String CFG_KAFKA_SCHEMA_REGISTRY_PORT = "schema.registry.port";
    public static final String CFG_KAFKA_CONNECTION_TIMEOUT_MS = "connection.timeout.ms";
    public static final String CFG_KAFKA_POLL_TIMEOUT_MS = "poll.timeout.ms";
    public static final String CFG_KAFKA_GROUP_ID = "group.id";

    public static final String CFG_MONGO_GROUP_COLLECTIONS = "Collections";

    public static final String CFG_FLOW_URL_PATH = "url.path";
    public static final String CFG_FLOW_TOPIC_SOURCE = "topic.source";
    public static final String CFG_FLOW_TRANSFORMER_CLASS = "transformer.class";
    public static final String CFG_FLOW_TRANSFORMER_REQUEST = "transformer.request";
    public static final String CFG_FLOW_TRANSFORMER_RESPONSE = "transformer.response";
    public static final String CFG_FLOW_TOPIC_RESPONSE_OK = "topic.response.ok";
    public static final String CFG_FLOW_TOPIC_RESPONSE_FAIL = "topic.response.fail";

    public static final String CFG_HTTP_TRANSFORMER_CLASS = "transformer.class";
    public static final String CFG_HTTP_TRANSFORMER_REQUEST = "transformer.request";

    public static final String CFG_CDC_TOPIC_NAME = "topic.name";
    public static final String CFG_CDC_GROUP_ID = "group.id";
    public static final String CFG_CDC_THRESHOLD = "cdc.threshold";

    private static final Object SYNC_LOCK = new Object();


    static ConfigSettings configSettings = null;
    static String configFileName = null;
    static String topicPrefix = null;
    private String storageError = null;

    private ConfigGroup root;
    private File configFile = null;

    public static void setConfigFile(String fileName) {
        configFileName = fileName;
        if (configSettings != null) {
            resetInstance();
        }
    }

    private static void resetInstance() {
        configSettings = null;
    }

    public static void resetInstanceForTests() {
        resetInstance();
    }

    public static ConfigSettings reloadConfigFromFile() {
        resetInstance();
        return getInstance();
    }

    /**
     * Allows setting a mock instance of ConfigSettings for testing purposes.
     * This method should only be used in test environments.
     *
     * @param mockInstance the mock instance of ConfigSettings
     */
    public static void setMockInstance(ConfigSettings mockInstance) {
        synchronized (SYNC_LOCK) {
            configSettings = mockInstance;
        }
    }

    public static ConfigSettings getInstance() {
        return getInstance(false);
    }

    public static ConfigSettings getInstance(boolean useDefaults) {
        if (configSettings == null) {
            synchronized (SYNC_LOCK) {
                initializeInstance(useDefaults);
            }
        }
        return configSettings;
    }

    private static void initializeInstance(boolean useDefaults) {
        configSettings = new ConfigSettings(useDefaults);
    }

    private ConfigSettings(boolean useDefaults) {
        createRoot(useDefaults);
    }

    void createRoot(boolean useDefaults) {
        read();     // read from config file.
        if (getStorageError() == null || !useDefaults) {
            return;
        }
        root = new ConfigGroup(ConfigType.Config, null);

        ConfigGroup presets = new ConfigGroup(ConfigType.Presets);
        root.addGroup(presets);

        ConfigGroup connGroup = new ConfigGroup(ConfigType.Http);
        connGroup.addItem(new ConfigItem(CFG_HTTP_URL, "http://<IP-address>:<port>/<common-path>/", "The URL of the HTTP receiver."));
        connGroup.addItem(new ConfigItem(CFG_HTTP_METHOD, "POST", "The method to use for sending."));
        connGroup.addItem(new ConfigItem(CFG_HTTP_CONNECT_TIMEOUT_MS, "5000", "The time it may take for connecting."));
        connGroup.addItem(new ConfigItem(CFG_HTTP_READ_TIMEOUT_MS, "10000", "The time it may take for reading (incl.connection time)."));
        presets.addGroup(connGroup);

        ConfigGroup kafkaGroup = new ConfigGroup(ConfigType.Kafka);
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_HOST, null, "Hostname/IP-address of the kafka broker"));
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_PORT, 9092));
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_SCHEMA_REGISTRY_PORT, 8081, "The port number of the schema registry"));
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_CONNECTION_TIMEOUT_MS, 10000, "Max time it may to take to connect"));
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_GROUP_ID, "aicon-tos-connect", "Group id allows multiple consumers to read in parallel"));
        kafkaGroup.addItem(new ConfigItem(CFG_KAFKA_POLL_TIMEOUT_MS, 1000, "Time for every poll session of the kafka stream"));
        presets.addGroup(kafkaGroup);

        ConfigGroup mongoGroup = new ConfigGroup(ConfigType.Mongo);
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_HOSTNAME, null, "Hostname/IP-address of the kafka broker"));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_HOSTPORT, 27017));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_USERNAME, null, "The user name"));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_PASSWORD, null, "The user password"));
        presets.addGroup(mongoGroup);

        ConfigGroup flowGroup = new ConfigGroup(ConfigType.Flow);
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_URL_PATH, "", "Extends the path of the connection URL"));
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_TOPIC_SOURCE, "", "The source topic to subscribe to"));
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_TOPIC_RESPONSE_OK, "", "The response topic to send to when a response has been received"));
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_TOPIC_RESPONSE_FAIL, "", "The response topic to send to when connection failed (when empty, the ok respone top[ic will be used."));
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_TRANSFORMER_REQUEST, "", "The transformer full classname (available on the classpath) to transform the request into a message the TOS accepts."));
        flowGroup.addItem(new ConfigItem(ConfigDomain.CFG_FLOW_TRANSFORMER_RESPONSE, "", "The transformer full classname (available on the classpath) to transform the response into a message Avlino accepts."));
        presets.addGroup(flowGroup);

        root.addGroup(new ConfigGroup(ConfigType.Connections));
        root.addGroup(new ConfigGroup(ConfigType.Flows));
        root.addGroup(new ConfigGroup(ConfigType.TosControl));
    }

    /**
     * Returns the root ConfigGroup containing all main config groups
     *
     * @return the root config group
     */
    public ConfigGroup getRoot() {
        return root;
    }

    /**
     * Returns all main Config groups residing directly under root.
     *
     * @param mainType the main ConfigType you want to retrieve
     * @return the top level ConfigGroup for given mainType
     */
    public ConfigGroup getMainGroup(ConfigType mainType) {
        return root != null ? root.getChildGroup(mainType) : null;
    }

    /**
     * Adds all missing ConfigItems in the given group. Items will be read from the ConfigGroup with this type
     * found in the presets.
     *
     * @param group the group to fill in.
     */
    public void addMissingPresetItems(ConfigGroup group) {
        if (group == null) {
            return;
        }
        ConfigGroup preset = root.getChildGroup(ConfigType.Presets).getChildGroup(group.getType());
        if (preset != null) {
            for (ConfigItem presetItem : preset.getItems()) {
                if (group.findItem(presetItem.key()) == null) {
                    group.addItem(new ConfigItem(presetItem));
                }
            }
        }
    }

    /**
     * Reads the properties from storage and provide the configuration object as is.
     *
     * @return null when ok, else the same text as in #getStorageError
     */
    public String read() {
        // Discard all current settings in memory and read from scratch
        ConfigGroup newRoot = new ConfigGroup(ConfigType.Config);
        setStorageError(newRoot.readXmlFile(ensureConfigFilesWithPath()));
        if (hasStorageError()) {
            LOG.error("Error while reading config file, reason: {}", getStorageError());
        } else {
            LOG.info("Replaced configuration root by file: {}", getFullFilename());
            root = newRoot;
        }
        return getStorageError();
    }

    /**
     * Save the properties to storage.
     *
     * @return null when ok, else the same text as in #getStorageError
     */
    public String save() {
        File file = ensureConfigFilesWithPath();
        setStorageError(root.storeXmlFile(file));
        if (hasStorageError()) {
            LOG.error("Error while saving to config file, reason: {}", getStorageError());
        }
        return getStorageError();
    }

    public String getFileName() {
        return configFileName;
    }

    public String getFullFilename() {
        String result;
        try {
            File file = ensureConfigFilesWithPath();
            if (file == null) {
                result = "no file defined, please check.";
            } else {
                result = file.getCanonicalPath();
            }
        } catch (IOException e) {
            result = String.format("Failure on file %s, reason: %s", configFileName, e);
        }
        return result;
    }


    public File getConfigFile() {
        return ensureConfigFilesWithPath();
    }


    File ensureConfigFilesWithPath() {
        if (configFileName == null) {
            String tml = AppComposer.getAppSetting(AppComposer.APP_VAR_TERMINAL);
            String env = AppComposer.getAppSetting(AppComposer.APP_VAR_ENVIRONMENT);
            if (StringUtils.hasContent(tml) && StringUtils.hasContent(env)) {
                // TODO Ron: Instance methods should not write to "static" fields
                configFileName = String.format("%s-%s-%s", tml, env, AICON_ENV_PROPS_FILENAME);
            } else {
                configFileName = AICON_ENV_PROPS_FILENAME;
            }
            configFileName = AICON_ENV_PROPS_CONF_PATH + configFileName;
            LOG.info("Using config file: {}", configFileName);
        }
        if (configFile == null) {
            configFile = ensureFileWithPath(configFileName);
            ensureFileWithPath(String.format("%s/%s", AICON_ENV_XSD_PATH, AICON_ENV_XSD_FILENAME));
        }
        return configFile;
    }

    // TODO Ron: Cognitive Complexity of methods should not be too hig
    private File ensureFileWithPath(String fileSpec) {
        File file = FileUtils.ensureFileWithPath(fileSpec);
        if (!file.exists()) {
            // copy from the classpath to the user-env
            ClassLoader cl = ConfigSettings.class.getClassLoader();
            String classFile = fileSpec.startsWith(FOLDER_UP) ? fileSpec.substring(2) : fileSpec;
            URL fileUrl = cl.getResource(classFile);
            if (fileUrl == null) {
                LOG.error("Config-file {} not found in user-env nor in classpath.", fileSpec);
            } else {
                try {
                    Path sourcePath = Paths.get(fileUrl.toURI()).toAbsolutePath();
                    Files.copy(sourcePath, file.toPath());
                    // Verify copy successfull, otherwise major error with configuration, application cannot function.
                    // Any way inform user via UI
                    if (!Files.exists(file.toPath())){
                        setStorageError(String.format("Error with creation new configuration file: %s.", file.toPath()));
                        if (LOG.isErrorEnabled()) {
                            LOG.error(AnsiColor.red(getStorageError()));
                        }
                    } else {
                        LOG.info("Config-file {} copied to {}.", sourcePath, file.toPath());
                    }
                } catch (Exception e) {
                    LOG.error("Copy of classpath config-file {} to user-env failed, reason: {}", fileSpec, e);
                }
            }
        }
        return file;
    }

    public boolean hasStorageError() {
        return getStorageError() != null;
    }

    /**
     * Returns the error when reading/writing from/to storage.
     *
     * @return if an error occurred, contains an error, else null.
     */
    public String getStorageError() {
        return storageError;
    }

    private void setStorageError(String message) {
        storageError = message;
    }
}
