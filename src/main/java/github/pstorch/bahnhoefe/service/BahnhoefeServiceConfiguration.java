package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderCh;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import github.pstorch.bahnhoefe.service.monitoring.LoggingMonitor;
import github.pstorch.bahnhoefe.service.monitoring.Monitor;
import github.pstorch.bahnhoefe.service.monitoring.SlackMonitor;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;

public class BahnhoefeServiceConfiguration extends Configuration {

    private final BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private final BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

    private Monitor monitor = new LoggingMonitor();

    public BahnhoefeLoaderDe getLoaderDe() {
        return loaderDe;
    }

    public BahnhoefeLoaderCh getLoaderCh() {
        return loaderCh;
    }

    public BahnhoefeRepository getRepository() {
        return new BahnhoefeRepository(monitor, loaderDe, loaderCh);
    }

    private String apiKey;

    private String uploadDir;

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
