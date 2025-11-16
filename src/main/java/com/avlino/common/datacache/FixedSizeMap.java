package com.avlino.common.datacache;

import java.util.LinkedHashMap;
import java.util.Map;

public class FixedSizeMap<K, V> extends LinkedHashMap<K, V> {
    private int maxSize;

    public FixedSizeMap(int maxSize) {
        super(maxSize, 0.75f, true); // true = access order (use false for insertion order)
        this.maxSize = maxSize;
    }

    public void setMaxSize(int newMaxSize) {
        maxSize = newMaxSize;
    }

    @Override
    protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
        return size() > maxSize;
    }
}
