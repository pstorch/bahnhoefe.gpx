package org.railwaystations.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.loader.BahnhoefeLoaderCh;
import org.railwaystations.api.loader.BahnhoefeLoaderDe;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.monitoring.LoggingMonitor;
import org.railwaystations.api.monitoring.Monitor;
import org.railwaystations.api.monitoring.SlackMonitor;

public class BahnhoefeServiceConfiguration extends Configuration {

    private static final String IDENT = "@class";

    private final BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private final BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();

    private Monitor monitor = new LoggingMonitor();

    private String apiKey;

    private UploadTokenGenerator uploadTokenGenerator;

    private String uploadDir;

    private Mailer mailer;

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

    public UploadTokenGenerator getUploadTokenGenerator() {
        return uploadTokenGenerator;
    }

    public void setSalt(final String salt) {
        this.uploadTokenGenerator = new UploadTokenGenerator(salt);
    }

    public Mailer getMailer() {
        return mailer;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = BahnhoefeServiceConfiguration.IDENT)
    public void setMailer(Mailer mailer) {
        this.mailer = mailer;
    }
}
