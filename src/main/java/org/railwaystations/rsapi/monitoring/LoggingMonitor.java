package org.railwaystations.rsapi.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
@Profile("dev")
public class LoggingMonitor implements Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMonitor.class);

    @Override
    public void sendMessage(final String message) {
        LOG.info(message);
    }

    @Override
    public void sendMessage(final String message, final File file) {
        LOG.info(message + " - " + file);
    }

}
