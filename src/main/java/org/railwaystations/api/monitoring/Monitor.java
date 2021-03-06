package org.railwaystations.api.monitoring;

import java.io.File;

public interface Monitor {
    void sendMessage(final String message);
    void sendMessage(final String message, File file);
}
