package org.railwaystations.rsapi;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.railwaystations.rsapi.auth.TokenGenerator;
import org.railwaystations.rsapi.mail.Mailer;
import org.railwaystations.rsapi.monitoring.Monitor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.io.File;

@SuppressWarnings("PMD.LongVariable")
public class RsApiConfiguration extends Configuration {

    private static final String IDENT = "@class";

    private Monitor monitor;

    private TokenGenerator tokenGenerator;

    private String workDir;

    private Mailer mailer;

    private String photoBaseUrl;

    private String inboxBaseUrl;

    private String mailVerificationUrl;

    private MastodonBot mastodonBot = new MastodonBot();

    @Valid
    @NotNull
    private DataSourceFactory database = new DataSourceFactory();

    @JsonProperty("database")
    public void setDataSourceFactory(final DataSourceFactory factory) {
        this.database = factory;
    }

    @JsonProperty("database")
    public DataSourceFactory getDataSourceFactory() {
        return database;
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = RsApiConfiguration.IDENT)
    public void setMonitor(final Monitor monitor) {
        this.monitor = monitor;
    }
}
