package com.avlino.common.datasources;

import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SqlConfigReader is a singleton instance which keeps the connection to the MySql Config server open until explicitly being
 * disconnected(). Keeps track of all relevant config items in the MySql config db.
 * For the time being we read directly from the config file, because to be honest, the config keys in config_field table are a mess.
 */
public class SqlConfigReader {
    private static final Logger LOG = LoggerFactory.getLogger(SqlConfigReader.class);

    public static final String CFG_HOSTNAME              = "hostname";
    public static final String CFG_HOSTPORT              = "hostport";
    public static final String CFG_USERNAME              = "username";
    public static final String CFG_PASSWORD              = "password";
    public static final String CFG_CONNECTION_TIMEOUT_MS = ConfigDomain.CFG_CONNECTION_TIMEOUT_MS;

    private static SqlConfigReader instance = null;
    private ConfigGroup configSql = null;
    private boolean connected = false;
    private String errorText = null;
    private long millis2Execute = 0;
    private static final Object lock = new Object();


    /**
     * @return the singleton instance of this class
     */
    public static SqlConfigReader getInstance() {
        if (instance == null) {
            synchronized (lock) {
                instance = new SqlConfigReader();
                instance.connect();
            }
        }
        return instance;
    }


    /**
     * Singleton constructor
     */
    private SqlConfigReader() {
    }

    /**
     * Connects to the Mongo server with the build-in configuration (normal app start).
     */
    private void connect() {
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup configDs = config.getMainGroup(ConfigType.Datasources);
        ConfigGroup localConfigSql = null;
        if (configDs != null) {
            localConfigSql = configDs.getChildGroup(ConfigType.SqlDb);
        }
        connect(localConfigSql);
    }

    /**
     * Connects to the mongo server using the given ConfigGroup with mongo items in it.
     *
     * @param configSql the ConfigGroup with details how to connect. See CFG_ constants above for keys.
     */
    private void connect(ConfigGroup configSql) {
        this.configSql = configSql;
        long startTime = System.currentTimeMillis();
        if (connected) {
            disconnect();
        }
        connected = false;

        if (configSql == null) {
            logErrorText("No %s config found under %s or even missing completely!", ConfigType.SqlDb, ConfigType.Datasources);
            return;
        }

        try {
            connected = true;
        } catch (Exception e) {
            errorText = String.format("SqlConfig connection failed, reason: %s", e);
            logErrorText(errorText);
        }
        millis2Execute = System.currentTimeMillis() - startTime;
    }

    public ConfigGroup getSubGroup(String groupName) {
        if (configSql != null) {
            return configSql.getChildGroup(groupName);
        }
        return null;
    }

    /**
     * @return true when a connection has been established
     */
    public boolean isConnected() {
        return connected;
    }

    /**
     * In case there is an error while connecting to or reading from mongo, the error text is returned here, else null.
     * @return null when no errors, else the error text of the last failure.
     */
    public String getErrorText() {
        return errorText;
    }

    public long getMillis2Execute() {
        return millis2Execute;
    }

    public void disconnect() {
        connected = false;
    }

    private void logErrorText(String formatText, Object... args) {
        errorText = String.format(formatText, args);
        LOG.error(errorText);
    }
}
