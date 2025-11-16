package com.aicon.tos.connect.flows;

import java.util.ArrayDeque;
import java.util.Deque;

public class FlowSessionManager {

    private final Deque<FlowSession> flowSessions;
    private long maxSize;

    public FlowSessionManager(int maxSize) {
        this.maxSize = maxSize;
        this.flowSessions = new ArrayDeque<>(100);
    }

    /**
     * Adds a new FlowSession to the list. If the list reaches the maximum size,
     * the oldest element is removed.
     *
     * @param session The FlowSession to add.
     */
    public synchronized void addSession(FlowSession session) {
        if (flowSessions.size() >= maxSize) {
            flowSessions.removeLast();
        }
        flowSessions.addFirst(session);
    }

    /**
     * Returns the most recent FlowSession without removing it.
     *
     * @return The most recent FlowSession, or null if the list is empty.
     */
    public synchronized FlowSession peekLatest() {
        return flowSessions.peekFirst();
    }

    /**
     * Removes and returns the most recent FlowSession.
     *
     * @return The most recent FlowSession, or null if the list is empty.
     */
    public synchronized FlowSession popLatest() {
        return flowSessions.pollFirst();
    }

    /**
     * Provides a list of all FlowSessions (most recent first).
     *
     * @return A list of FlowSessions.
     */
    public synchronized Deque<FlowSession> getAllSessions() {
        return new ArrayDeque<>(flowSessions);
    }

    /**
     * Returns the number of FlowSessions currently in the list.
     *
     * @return The number of FlowSessions.
     */
    public synchronized int size() {
        return flowSessions.size();
    }

    /**
     * Clears the list.
     */
    public synchronized void clear() {
        flowSessions.clear();
    }

    /**
     * Updates the maximum size of the list. If the list size exceeds the new maximum,
     * the oldest elements are removed until the size is within the new limit.
     *
     * @param newSize The new maximum size.
     */
    public void setSize(int newSize) {
        this.maxSize = newSize;

        while (flowSessions.size() >= maxSize) {
            flowSessions.removeLast();
        }
    }
}