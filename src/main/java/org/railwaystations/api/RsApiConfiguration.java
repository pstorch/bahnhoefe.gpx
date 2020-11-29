package org.railwaystations.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.RsaJsonWebKey;
import org.jose4j.lang.JoseException;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.monitoring.LoggingMonitor;
import org.railwaystations.api.monitoring.Monitor;
import org.railwaystations.api.monitoring.SlackMonitor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("PMD.LongVariable")
public class RsApiConfiguration extends Configuration {

    private static final String IDENT = "@class";

    private Monitor monitor = new LoggingMonitor();

    private TokenGenerator tokenGenerator;

    private String workDir;

    private Mailer mailer;

    private String slackVerificationToken;

    private String photoBaseUrl;

    private String inboxBaseUrl;

    private String mailVerificationUrl;

    private MastodonBot mastodonBot = new MastodonBot();

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    private Map<String, Object> jwkParams = new HashMap<>();

    @JsonProperty("database")
    public void setDataSourceFactory(final DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
    }

    public void setSlackMonitorUrl(final String slackMonitorUrl) {
        if (StringUtils.isNotBlank(slackMonitorUrl)) {
            this.monitor = new SlackMonitor(slackMonitorUrl);
        }
    }

    public Monitor getMonitor() {
        return monitor;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setWorkDir(final String workDir) {
        this.workDir = workDir;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = RsApiConfiguration.IDENT)
    public void setMailer(final Mailer mailer) {
        this.mailer = mailer;
    }

    public String getSlackVerificationToken() {
        return slackVerificationToken;
    }

    public void setSlackVerificationToken(final String slackVerificationToken) {
        this.slackVerificationToken = slackVerificationToken;
    }

    public String getPhotoBaseUrl() {
        return photoBaseUrl;
    }

    public void setPhotoBaseUrl(final String photoBaseUrl) {
        this.photoBaseUrl = photoBaseUrl;
    }

    public String getInboxBaseUrl() {
        return inboxBaseUrl;
    }

    public void setInboxBaseUrl(final String inboxBaseUrl) {
        this.inboxBaseUrl = inboxBaseUrl;
    }

    public String getPhotosDir() {
        return getWorkDir() + File.separator + "photos";
    }

    public String getInboxDir() {
        return getWorkDir() + File.separator + "inbox";
    }

    public String getInboxProcessedDir() {
        return getInboxDir() + File.separator + "processed";
    }

    public String getInboxToProcessDir() {
        return getInboxDir() + File.separator + "toprocess";
    }

    public String getMailVerificationUrl() {
        return mailVerificationUrl;
    }

    public void setMailVerificationUrl(final String mailVerificationUrl) {
        this.mailVerificationUrl = mailVerificationUrl;
    }

    public MastodonBot getMastodonBot() {
        return mastodonBot;
    }

    public void setMastodonBot(final MastodonBot mastodonBot) {
        this.mastodonBot = mastodonBot;
    }

    public Map<String, Object> getJwkParams() {
        return jwkParams;
    }

    public RsaJsonWebKey getRsaJsonWebKey() {
        try {
            return (RsaJsonWebKey) JsonWebKey.Factory.newJwk(jwkParams);
        } catch (final JoseException e) {
            throw new RuntimeException("Unable to create RsaJsonWebKey", e);
        }
    }

}
