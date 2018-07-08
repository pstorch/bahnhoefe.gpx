package org.railwaystations.api.monitoring;

public interface Monitor {
    void sendMessage(final String message);

    void sendMessage(final String responseUrl, final String message);
}
