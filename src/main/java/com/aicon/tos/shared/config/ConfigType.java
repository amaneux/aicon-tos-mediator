package com.aicon.tos.shared.config;

import java.util.Arrays;
import java.util.Map;

public enum ConfigType {
    Config      , ConfigGroup,
    Presets     ,
    General     , GeneralItems,
    Datasources , Mongo, SqlDb,
    Connections , Kafka, Http, HttpRef, KafkaRef, MongoRef,
    Flows       , Flow, Source, Transform, Sink, Response,
    TosControl  , Control, CanaryCheck, CDCTable, ControlRequest, DeckingUpdateRequest,
    Interceptors, InterceptorEntity, Scenarios, Scenario;

    public static final ConfigType[] COMPONENTS         = {Datasources, Connections, Flows, TosControl};
    public static final ConfigType[] CONNECTION_TYPES   = {Kafka, Http};
    public static final ConfigType[] FLOW_STEPS         = {Source, Transform, Sink, Response};

    public static final String CONTENT_TYPE_XML         = "XML";
    public static final String CONTENT_TYPE_JSON        = "JSON";

    private static final Map<ConfigType, ConfigType[]> RELATED_TYPES = Map.of(
            HttpRef, new ConfigType[]{Http},
            KafkaRef, new ConfigType[]{Kafka},
            Http, new ConfigType[]{HttpRef},
            Kafka, new ConfigType[]{KafkaRef}
    );

    public boolean matches(ConfigType other) {
        if (this == other) {
            return true;
        }
        ConfigType[] related = RELATED_TYPES.get(this);
        return related != null && Arrays.asList(related).contains(other);
    }
}
