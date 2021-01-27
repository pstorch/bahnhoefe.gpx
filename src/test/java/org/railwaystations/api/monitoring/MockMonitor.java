package org.railwaystations.api.monitoring;

import java.util.ArrayList;
import java.util.List;

public class MockMonitor implements Monitor {

    private final List<String> messages = new ArrayList<>();

    @Override
    public void sendMessage(final String message) {
        messages.add(message);
    }

    public List<String> getMessages() {
        return messages;
    }

}
