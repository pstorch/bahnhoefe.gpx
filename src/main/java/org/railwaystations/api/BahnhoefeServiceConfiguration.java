package org.railwaystations.api;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.loader.*;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.monitoring.LoggingMonitor;
import org.railwaystations.api.monitoring.Monitor;
import org.railwaystations.api.monitoring.SlackMonitor;

@SuppressWarnings("PMD.LongVariable")
public class BahnhoefeServiceConfiguration extends Configuration {

    private static final String IDENT = "@class";

    private final BahnhoefeLoaderDe loaderDe = new BahnhoefeLoaderDe();
    private final BahnhoefeLoaderCh loaderCh = new BahnhoefeLoaderCh();
    private final BahnhoefeLoaderFi loaderFi = new BahnhoefeLoaderFi();
    private final BahnhoefeLoaderUk loaderUk = new BahnhoefeLoaderUk();
    private final BahnhoefeLoaderFr loaderFr = new BahnhoefeLoaderFr();
    private final BahnhoefeLoaderEs loaderEs = new BahnhoefeLoaderEs();

    private Monitor monitor = new LoggingMonitor();

    private String apiKey;

    private TokenGenerator tokenGenerator;

    private String uploadDir;

    private Mailer mailer;

    private String slackVerificationToken;

    public BahnhoefeLoaderDe getLoaderDe() {
        return loaderDe;
    }

    public BahnhoefeLoaderCh getLoaderCh() {
        return loaderCh;
    }

    public BahnhoefeLoaderFi getLoaderFi() {
        return loaderFi;
    }

    public BahnhoefeLoaderUk getLoaderUk() {
        return loaderUk;
    }

    public BahnhoefeRepository getRepository() {
        return new BahnhoefeRepository(monitor, loaderDe, loaderCh, loaderFi, loaderUk, loaderFr, loaderEs);
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

    public TokenGenerator getTokenGenerator() {
        return tokenGenerator;
    }

    public void setSalt(final String salt) {
        this.tokenGenerator = new TokenGenerator(salt);
    }

    public Mailer getMailer() {
        return mailer;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = BahnhoefeServiceConfiguration.IDENT)
    public void setMailer(final Mailer mailer) {
        this.mailer = mailer;
    }

    public String getSlackVerificationToken() {
        return slackVerificationToken;
    }

    public void setSlackVerificationToken(final String slackVerificationToken) {
        this.slackVerificationToken = slackVerificationToken;
    }

    public BahnhoefeLoaderFr getLoaderFr() {
        return loaderFr;
    }

    public BahnhoefeLoaderEs getLoaderEs() {
        return loaderEs;
    }
}
