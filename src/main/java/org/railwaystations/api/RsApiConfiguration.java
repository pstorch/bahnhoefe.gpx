package org.railwaystations.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.dropwizard.Configuration;
import io.dropwizard.db.DataSourceFactory;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.loader.PhotographerLoader;
import org.railwaystations.api.loader.StationLoaderFactory;
import org.railwaystations.api.mail.Mailer;
import org.railwaystations.api.monitoring.LoggingMonitor;
import org.railwaystations.api.monitoring.Monitor;
import org.railwaystations.api.monitoring.SlackMonitor;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("PMD.LongVariable")
public class RsApiConfiguration extends Configuration {

    private static final String IDENT = "@class";

    private Monitor monitor = new LoggingMonitor();

    private String apiKey;

    private TokenGenerator tokenGenerator;

    private String workDir;

    private Mailer mailer;

    private String slackVerificationToken;

    private String photographersUrl;

    private String photoBaseUrl;

    private ElasticBackend elasticBackend;

    @JsonProperty
    @NotNull
    @Valid
    private List<StationLoaderFactory> loaders;

    private String photoDir;

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

    public StationsRepository getRepository() {
        return new StationsRepository(monitor, loaders.stream().map(factory -> factory.createLoader(monitor, elasticBackend)).collect(Collectors.toList()), getPhotographerLoader(), photoBaseUrl);
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = RsApiConfiguration.IDENT)
    public void setMailer(final Mailer mailer) {
        this.mailer = mailer;
    }

    public String getSlackVerificationToken() {
        return slackVerificationToken;
    }

    public void setSlackVerificationToken(final String slackVerificationToken) {
        this.slackVerificationToken = slackVerificationToken;
    }

    public PhotographerLoader getPhotographerLoader() {
        return new PhotographerLoader(photographersUrl, elasticBackend);
    }

    public void setPhotographersUrl(final String photographersUrl) {
        this.photographersUrl = photographersUrl;
    }

    public String getPhotoBaseUrl() {
        return photoBaseUrl;
    }

    public void setPhotoBaseUrl(final String photoBaseUrl) {
        this.photoBaseUrl = photoBaseUrl;
    }

    public String getPhotoDir() {
        return photoDir;
    }

    public void setPhotoDir(final String photoDir) {
        this.photoDir = photoDir;
    }

    public ElasticBackend getElasticBackend() {
        return elasticBackend;
    }

    public void setElasticBackend(final ElasticBackend elasticBackend) {
        this.elasticBackend = elasticBackend;
    }
}
