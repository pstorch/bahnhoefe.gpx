package org.railwaystations.api.monitoring;

public interface Monitor {
    void sendMessage(final String message);

    void sendMessage(String responseUrl, String message);
}
