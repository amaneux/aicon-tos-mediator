package com.aicon.tos.interceptor.decide;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

public class ResponseHandler<V> {
    private static final Logger LOG = LoggerFactory.getLogger(ResponseHandler.class);
    private final String key;
    private final CompletableFuture<V> future = new CompletableFuture<>();

    public ResponseHandler(String key) {
        this.key = key;
    }

    public void completeIfMatch(String responseKey, V response) {
        if (key.equals(responseKey)) {
            future.complete(response);
            LOG.debug("Completed future for key: {}", key);
        }
    }

    public void completeExceptionally(Throwable ex) {
        future.completeExceptionally(ex);
        LOG.error("Completed future exceptionally for key: {}", key, ex);
    }

    public CompletableFuture<V> getFuture() {
        return future;
    }
}

