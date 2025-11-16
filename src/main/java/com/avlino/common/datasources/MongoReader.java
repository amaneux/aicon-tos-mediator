package com.avlino.common.datasources;

import com.aicon.model.MoveInfo;
import com.aicon.tos.ConfigDomain;
import com.aicon.tos.shared.config.ConfigGroup;
import com.aicon.tos.shared.config.ConfigItem;
import com.aicon.tos.shared.config.ConfigSettings;
import com.aicon.tos.shared.config.ConfigType;
import com.avlino.common.Constants;
import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * MongoReader is a singleton instance which keeps the connection to the Mongo server open until explicitly being
 * disconnected(). Provides configurable services to look up a database and collection combi and caches the MongoCollection
 * based on the database+collection as given via a unique key. Documents are read via Queries on the (cached) collection.
 */
public class MongoReader {
    private static final Logger LOG = LoggerFactory.getLogger(MongoReader.class);

    public static final String CFG_HOSTNAME              = "hostname";
    public static final String CFG_HOSTPORT              = "hostport";
    public static final String CFG_USERNAME              = "username";
    public static final String CFG_PASSWORD              = "password";
    public static final String CFG_CONNECTION_TIMEOUT_MS = ConfigDomain.CFG_CONNECTION_TIMEOUT_MS;

    private static final String URL_FORMAT               = "mongodb://%s:%s@%s:%s";
    private static final String DB_ADMIN                 = "admin";     // to test the connection
    private static final String COMMAND_PING             = "ping";      // by using this command
    private static final Object lock = new Object();

    private static final String ID_GKEY = "_id.gkey";
    private static final String ID_WI_GKEY = "wi_gkey";

    private static MongoReader instance = null;
    private boolean connected = false;
    private String errorText = null;
    private long millis2Execute = 0;

    private MongoClient mongoClient = null;
    private Map<String,MongoCollection<Document>> collectionCache = null;
    private ConfigGroup configCollections = null;


    /**
     * @return the singleton instance of this class
     */
    public static MongoReader getInstance() {
        if (instance == null) {
            synchronized (lock) {
                instance = new MongoReader();
                instance.connect();
            }
        }
        return instance;
    }


    /**
     * To test the MongoReader independent of its normal enironment.
     * @param args not needed
     */
    public static void main(String[] args) {
        String collectionKey = MoveInfo.MONGO_COLLECTION_MOVE_INFO;
        ConfigGroup mongoGroup = new ConfigGroup(ConfigType.Mongo);
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_HOSTNAME, "dct-dev-mongo.avlino.az", "Hostname/IP-address of the kafka broker"));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_HOSTPORT, 27018));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_USERNAME, "alenza", "The user name"));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_PASSWORD, "Thisis4devops", "The user password"));
        mongoGroup.addItem(new ConfigItem(MongoReader.CFG_CONNECTION_TIMEOUT_MS, "6000", "The connection timeout"));
        ConfigGroup collGroup = new ConfigGroup(ConfigType.Mongo, ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
        collGroup.addItem(new ConfigItem(collectionKey, "current_state_v2/vw_RTE_Move_Info"));
        mongoGroup.addGroup(collGroup);

        MongoReader mongo = new MongoReader();
        if (mongo.connect(mongoGroup)) {
            LOG.info("Connected to MongoDB in {} ms", mongo.getMillis2Execute());

            // 1st read
            long keyValue = 18757601L;
            long startTime = System.currentTimeMillis();
            Document query = new Document(ID_GKEY, keyValue);
            Document result = mongo.readDocument(mongo.getCollection(collectionKey), query);
            showResult(mongo, keyValue, result, startTime);

            // 2nd read
            keyValue = 18961740L;
            startTime = System.currentTimeMillis();
            query = new Document(ID_GKEY, keyValue);
            result = mongo.readDocument(mongo.getCollection(collectionKey), query);
            showResult(mongo, keyValue, result, startTime);

            // 3rd read using wi_gkey this time
            keyValue = 18961724L;
            startTime = System.currentTimeMillis();
            query = new Document(ID_WI_GKEY, keyValue);
            result = mongo.readDocument(mongo.getCollection(collectionKey), query);
            showResult(mongo, keyValue, result, startTime);

            // 4th read returning nothing
            keyValue = 1L;
            startTime = System.currentTimeMillis();
            query = new Document(ID_GKEY, keyValue);
            result = mongo.readDocument(mongo.getCollection(collectionKey), query);
            showResult(mongo, keyValue, result, startTime);
        }
    }

    private static void showResult(MongoReader mongo, long keyValue, Document result, long startTime) {
        if (result != null) {
            String log = String.format("Found document in %s ms with (some) values: container_id=%s, active_ufv=%s, wi_gkey=%s, move_kind=%s, pos_name=%s",
                    System.currentTimeMillis() - startTime,
                    result.getString("container_id"),
                    result.getLong("active_ufv"),
                    result.getLong(ID_WI_GKEY),
                    result.getString("move_kind"),
                    result.getString("pos_name")
            );
            LOG.info(log);
        } else {
            mongo.logErrorText("No document found for key: %s, reason: %s", keyValue, mongo.getErrorText());
        }
    }

    /**
     * Singleton constructor
     */
    private MongoReader() {
    }

    /**
     * Connts to the Mongo server with the build-in configuration (normal app start).
     */
    private void connect() {
        ConfigSettings config = ConfigSettings.getInstance();
        ConfigGroup configDs = config.getMainGroup(ConfigType.Datasources);
        ConfigGroup configMongo = null;
        if (configDs != null) {
            configMongo = configDs.getChildGroup(ConfigType.Mongo);
        }
        connect(configMongo);
    }

    /**
     * Connects to the mngo server using the given ConfigGroup with mongo items in it.
     * @param configMongo the ConfigGroup with detaisl hnow to connect. See CFG_ constants above for keys.
     */
    private boolean connect(ConfigGroup configMongo) {
        long startTime = System.currentTimeMillis();
        if (connected && mongoClient != null) {
            disconnect();
        }
        connected = false;

        if (configMongo == null) {
            logErrorText("No %s config found under %s or even missing completely!", ConfigType.Mongo, ConfigType.Datasources);
            return connected;
        }

        String url = null;
        try {
            url = String.format(URL_FORMAT
                    ,configMongo.getItemValue(MongoReader.CFG_USERNAME, "<user>")
                    ,configMongo.getItemValue(MongoReader.CFG_PASSWORD, "<pwd>")
                    ,configMongo.getItemValue(MongoReader.CFG_HOSTNAME, "<host>")
                    ,configMongo.getItemValue(MongoReader.CFG_HOSTPORT, "<port>")
            );
            int toSecs = Integer.parseInt(configMongo.getItemValue(MongoReader.CFG_CONNECTION_TIMEOUT_MS, "30000"));
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(new ConnectionString(url))
                    .applyToSocketSettings(builder -> builder.connectTimeout(toSecs, TimeUnit.MILLISECONDS))
                    .applyToClusterSettings(builder ->  builder.serverSelectionTimeout(toSecs, TimeUnit.MILLISECONDS))
                    .build();

            mongoClient = MongoClients.create(settings);

            MongoDatabase database = mongoClient.getDatabase(DB_ADMIN);
            database.runCommand(new Document(COMMAND_PING, 1));
            configCollections = configMongo.getChildGroup(ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
            if (configCollections == null) {
                logErrorText("No ChildGroup configured named %s under %s", ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS, ConfigType.Mongo);
            }
            connected = true;
            errorText = null;
            collectionCache = new HashMap<>();
        } catch (Exception e) {
            errorText = String.format("Mongo connection failed using %s, reason: %s", url, e);
            logErrorText(errorText);
        }
        millis2Execute = System.currentTimeMillis() - startTime;
        return connected;
    }

    /**
     * @return true when connection has been established
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

    /**
     * Gets the collection from cache or else trying to find it using the given db/collection String.
     * @param dbCollection the database/collection look up string
     * @return the collection from cache if found earlier, else the just found collection. If nothing, then null.
     */
    public MongoCollection<Document> getCollection(String dbCollection) {
        long startTime = System.currentTimeMillis();
        try {
            if (!connected) {
                connect();
                if (!connected) {
                    return null;
                }
            }

            MongoCollection<Document> mongoColl = getCachedCollection(dbCollection);
            if (mongoColl != null) {
                return mongoColl;
            }

            if (configCollections == null) {
                return null;
            }
            ConfigItem collItem = configCollections.findItem(dbCollection);
            if (collItem == null) {
                logErrorText("No config found for %s in child group named %s", dbCollection, ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
                return null;
            }
            String[] parts = collItem.value().split(Constants.REGEX_SLASH);
            if (parts.length != 2) {
                logErrorText("ConfigItem %s under %s not properly set, expecting database/collection as a value", dbCollection, ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
                return null;
            }
            MongoDatabase mongoDb = mongoClient.getDatabase(parts[0]);
            if (mongoDb == null) {
                logErrorText("No database found for %s under %s", dbCollection, ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
            }
            mongoColl = mongoDb.getCollection(parts[1]);
            if (mongoColl == null) {
                logErrorText("No collection found for %s under %s", dbCollection, ConfigSettings.CFG_MONGO_GROUP_COLLECTIONS);
            } else {
                collectionCache.put(dbCollection, mongoColl);
            }
            millis2Execute = System.currentTimeMillis() - startTime;
            return mongoColl;
        } catch (Exception e) {
            logErrorText("Could not open db/collection %s, reason: %s", dbCollection, e);
            millis2Execute = System.currentTimeMillis() - startTime;
            return null;
        }
    }

    /**
     * Reads the first document within given collection using the <code>docKey</code> and query to look for.
     * @param query the query document using key & value the mongo way
     * @return the first document found or null when nothing found.
     */
    public Document readDocument(MongoCollection<Document> collection, Document query) {
        long startTime = System.currentTimeMillis();
        if (collection == null || query == null) {
            return null;
        }
        Document result = null;
        try {
            result = collection.find(query).first();
            if (result != null) {
                if (LOG.isInfoEnabled()) {
                    LOG.info("Found document: {}", result.toJson());
                }
            } else {
                logErrorText("No document found for query: %s", query);
            }
        } catch (Exception e) {
            logErrorText("Reading document for query %s failed, reason: %s", query, e);
        }
        millis2Execute = System.currentTimeMillis() - startTime;
        return result;
    }

    /**
     * Ensures the cache when not available and returns the collection when found.
     * @param dbCollection the key to the collection (db/collection)
     * @return null if not found or else the collection ready for retrieving document(s) from.
     */
    private MongoCollection<Document> getCachedCollection(String dbCollection) {
        if (collectionCache == null) {
            collectionCache = new HashMap<>();
        }
        return collectionCache.get(dbCollection);
    }

    public void disconnect() {
        try {
            if (mongoClient != null) {
                mongoClient.close();
            }
        } catch (Exception e) {
            logErrorText("Failed to close Mongo connection, reason: %s", e);
        }
        connected = false;
        collectionCache = new HashMap<>();
    }


    private void logErrorText(String formatText, Object... args) {
        errorText = String.format(formatText, args);
        LOG.error(errorText);
    }
}
