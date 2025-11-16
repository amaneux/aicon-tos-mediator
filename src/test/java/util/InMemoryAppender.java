package util;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = Collections.synchronizedList(new ArrayList<>());
    private final String name;

    // Constructor that allows adding a name to the appender
    public InMemoryAppender(String name) {
        this.name = name;
    }

    @Override
    protected void append(ILoggingEvent eventObject) {
        System.out.println("[DynamicInMemoryAppender] Captured log event: " + eventObject.getFormattedMessage());
        events.add(eventObject);
    }

    // Return the name of the appender
    public String getName() {
        return name;
    }

    // Return all captured log messages
    public List<String> getLogMessages() {
        synchronized (events) {
            List<String> messages = new ArrayList<>();
            for (ILoggingEvent event : events) {
                messages.add(event.getFormattedMessage());
            }
            return messages;
        }
    }

    // Check whether the log contains a specific message
    public boolean contains(String substring) {
        return getLogMessages().stream().anyMatch(msg -> msg.contains(substring));
    }

    // Clear all captured events
    public void clear() {
        events.clear();
    }
}