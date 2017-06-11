package org.railwaystations.api.monitoring;

import org.junit.Test;

public class SlackMonitorTest {

    /**
     * To run this test against a real Slack channel, set the URL
     */
    private final String SLACK_URL = "";

    @Test
    public void sendMessage() {
        final SlackMonitor slack = new SlackMonitor(SLACK_URL);
        //slack.sendMessageInternal("Ein Test mit Umlauten öäüßÖÄÜ");
    }

}
