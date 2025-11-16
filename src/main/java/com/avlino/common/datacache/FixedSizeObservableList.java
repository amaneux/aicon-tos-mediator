package com.avlino.common.datacache;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Consumer;

/**
 * Extends an ArrayList with max size, reversed storage order and observable capabilities.
 * @param <E> the class for this List.
 */
public class FixedSizeObservableList<E> extends ArrayList<E> {

    public enum ListOrder {
        ASCENDING,
        DESCENDING
    }
    private static final Logger LOG = LoggerFactory.getLogger(FixedSizeObservableList.class);

    private int maxSize;
    private boolean isAscending;
    private final List<Consumer<List<E>>> observers = new ArrayList<>();

    public FixedSizeObservableList(int maxSize, ListOrder order) {
        super(maxSize);
        this.maxSize = maxSize;
        this.isAscending = order == ListOrder.ASCENDING;
    }
    public void addObserver(Consumer<List<E>> observer) {
        if (observer != null) {
            observers.add(observer);
        }
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    @Override
    public boolean contains(Object o) {
        return super.contains(o);
    }

    @Override
    public boolean add(E e) {
        boolean added;
        if (size() == maxSize) {
            if (isAscending) {
                super.remove(0);    // to avoid onChange 2 times
            } else {
                super.remove(size() - 1);
            }
        }
        if (isAscending) {
            added = super.add(e);
        } else {
            super.add(0, e);
            added = true;
        }
        notifyObservers();
        return added;
    }

    @Override
    public boolean remove(Object o) {
        boolean result = super.remove(o);
        notifyObservers();
        return result;
    }

    /**
     * Notify any subscribed observer to a relevant change in this list. For add() and remove() it is already happening.
     */
    public void notifyObservers() {
        for (Consumer<List<E>> observer : observers) {
            if (observer != null) {
                try {
                    observer.accept(this);
                } catch (Exception ex) {
                    LOG.error("Observer failed: {}", ex.getMessage());
                }
            }
        }
    }
}
