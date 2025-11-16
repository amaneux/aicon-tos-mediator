package com.aicon.model;

import com.aicon.tos.interceptor.MessageMeta;
import com.aicon.tos.interceptor.decide.scenarios.n4.events.WorkInstructionEvent;
import com.avlino.common.datasources.MongoReader;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import static com.avlino.common.Constants.UNKNOWN;

public class MoveInfo {
    private static final Logger LOG = LoggerFactory.getLogger(MoveInfo.class);

    public static final String MONGO_COLLECTION_MOVE_INFO = "collection.move_info";
    private static final String MONGO_ID_GKEY = "wi_gkey";
    private static final String MONGO_CONTAINER_ID = "container_id";
    private static final String MONGO_ACTIVE_UFV = "active_ufv";
    private static final String MONGO_CONTAINER_ISO_CODE = "container_iso_code";
    private static final String MONGO_CONTAINER_ISO_GROUP = "container_iso_group";
    private static final String MONGO_IS_EMPTY_CONTAINER = "is_empty_container";
    private static final String MONGO_MOVE_KIND = "move_kind";

    public static final String FLD_CTR_ID = "containerNumber";
    public static final String FLD_CTR_ISO_CODE = "containerISOCode";
    public static final String FLD_CTR_ISO_GROUP = "containerISOGroup";
    public static final String FLD_CTR_IS_EMPTY = "isEmptyContainer";
    public static final String FLD_CTR_VISIT_ID = "containerVisitId";
    public static final String FLD_WI_MOVE_KIND = "wiMoveKind";

    static public Map<String, Object> readMoveInfo(long wiGkey, MessageMeta messageMeta) {
        Map<String, Object> containerInfo = new java.util.HashMap<>();

        if (messageMeta != null) {
            messageMeta.addTimestampWithPrefix(MessageMeta.TS_READ_PREFIX, "mongo." + MONGO_COLLECTION_MOVE_INFO, LOG);
        }
        MongoReader mongo = MongoReader.getInstance();
        Document query = new Document(MONGO_ID_GKEY, wiGkey);
        Document result = mongo.readDocument(mongo.getCollection(MONGO_COLLECTION_MOVE_INFO), query);
        if (result == null) {
            LOG.error("Container info not found for {}, reason: {}", wiGkey, mongo.getErrorText());
        } else {
            LOG.info("Mongo-query ok for wi-gkey {}, found ctr-id {}.", wiGkey, result.getString(MONGO_CONTAINER_ID));
        }
        containerInfo.put(FLD_CTR_ID        , result == null ? UNKNOWN : result.getString(MONGO_CONTAINER_ID));
        containerInfo.put(FLD_CTR_VISIT_ID  , result == null ? UNKNOWN : String.valueOf(result.getLong(MONGO_ACTIVE_UFV)));
        containerInfo.put(FLD_CTR_ISO_CODE  , result == null ? UNKNOWN : result.getString(MONGO_CONTAINER_ISO_CODE));
        containerInfo.put(FLD_CTR_ISO_GROUP , result == null ? UNKNOWN : result.getString(MONGO_CONTAINER_ISO_GROUP));
        containerInfo.put(FLD_CTR_IS_EMPTY  , result == null ? false   : result.getBoolean(MONGO_IS_EMPTY_CONTAINER));
        containerInfo.put(FLD_WI_MOVE_KIND  , result == null ? UNKNOWN : result.getString(MONGO_MOVE_KIND));

        return containerInfo;
    }
}
