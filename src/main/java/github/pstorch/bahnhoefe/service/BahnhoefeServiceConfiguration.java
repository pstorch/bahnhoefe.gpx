package github.pstorch.bahnhoefe.service;

import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderCh;
import github.pstorch.bahnhoefe.service.loader.BahnhoefeLoaderDe;
import github.pstorch.bahnhoefe.service.monitoring.LoggingMonitor;
import github.pstorch.bahnhoefe.service.monitoring.Monitor;
import github.pstorch.bahnhoefe.service.monitoring.SlackMonitor;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;

public class BahnhoefeServiceConfiguration extends Configuration {

    private BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

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

    public void setSlackMonitorUrl(final String slackMonitorUrl) {
        if (StringUtils.isNotBlank(slackMonitorUrl)) {
            this.monitor = new SlackMonitor(slackMonitorUrl);
        }
    }

    public Monitor getMonitor() {
        return monitor;
    }

}
