package org.railwaystations.api.monitoring;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;

public class SlackMonitorTest {

    /**
     * To run this test against a real Slack channel, set the URL
     */
    private static final String SLACK_URL = "";

    @Test
    public void sendMessage() {
        final SlackMonitor slack = new SlackMonitor(SLACK_URL);
        if (StringUtils.isNoneEmpty(SLACK_URL)) {
            slack.sendMessageInternal("Ein Test mit Umlauten öäüßÖÄÜ", SLACK_URL);
        }
    }

}
