package github.pstorch.bahnhoefe.service.monitoring;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LoggingMonitor implements Monitor {

    private static final Logger LOG = LoggerFactory.getLogger(LoggingMonitor.class);

    @Override
    public void sendMessage(final String message) {
        LOG.info(message);
    }

}
