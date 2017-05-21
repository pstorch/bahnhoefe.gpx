package org.railwaystations.api;

import org.railwaystations.api.loader.BahnhoefeLoaderCh;
import org.railwaystations.api.loader.BahnhoefeLoaderDe;
import org.railwaystations.api.monitoring.LoggingMonitor;
import org.railwaystations.api.monitoring.Monitor;
import org.railwaystations.api.monitoring.SlackMonitor;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;

public class BahnhoefeServiceConfiguration extends Configuration {

    private final BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private final BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

    private Monitor monitor = new LoggingMonitor();

    private String apiKey;

    private String uploadDir;

    public BahnhoefeLoaderDe getLoaderDe() {
        return loaderDe;
    }

    public BahnhoefeLoaderCh getLoaderCh() {
        return loaderCh;
    }

    public BahnhoefeRepository getRepository() {
        return new BahnhoefeRepository(monitor, loaderDe, loaderCh);
    }

    public void setSlackMonitorUrl(final String slackMonitorUrl) {
        if (StringUtils.isNotBlank(slackMonitorUrl)) {
            this.monitor = new SlackMonitor(slackMonitorUrl);
        }
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(final String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(final String apiKey) {
        this.apiKey = apiKey;
    }

}
