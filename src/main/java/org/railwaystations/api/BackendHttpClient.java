package org.railwaystations.api;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.function.Function;

public class BackendHttpClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final JsonFactory FACTORY = MAPPER.getFactory();
    private static final String HITS_ELEMENT = "hits";

    private static final Logger LOG = LoggerFactory.getLogger(BackendHttpClient.class);
    private static final int BATCH_SIZE = 1000;

    private final CloseableHttpClient httpclient;

    public BackendHttpClient() {
        super();
        this.httpclient = HttpClients.custom().setDefaultRequestConfig(
                RequestConfig.custom()
                        .setSocketTimeout(5000)
                        .setConnectTimeout(5000)
                        .setConnectionRequestTimeout(15000).build()
        ).build();
    }

    public JsonNode readJsonFromUrl(final URL url) throws Exception {
        // shortcut for testing, need to find a better way
        if ("file".equals(url.getProtocol())) {
            return MAPPER.readTree(url);
        }

        // use Apache HTTP Client to retrieve remote content
        final HttpGet httpGet = new HttpGet(url.toURI());
        return httpclient.execute(httpGet, response -> {
            final int status = response.getStatusLine().getStatusCode();
            if (status >= 200 && status < 300) {
                LOG.info("Got json response from {}", url);
                return MAPPER.readTree(FACTORY.createParser(EntityUtils.toString(response.getEntity(), "UTF-8")));
            } else {
                LOG.error("Error reading json from {}", url);
                throw new ClientProtocolException(String.format("Unexpected response status: %d", status));
            }
        });
    }

    public void fetchAll(final URL url, final int from, final Function<JsonNode, Void> readHits) throws Exception {
        final JsonNode hits = readJsonFromUrl(new URL(url + "?size=" + BATCH_SIZE + "&from=" + from))
                .get(HITS_ELEMENT);
        final int total = hits.get("total").asInt();
        readHits.apply(hits.get(HITS_ELEMENT));
        if (total > from + BATCH_SIZE) {
            fetchAll(url, from + BATCH_SIZE, readHits);
        }
    }

    public CloseableHttpResponse post(final URL url, final String content, final ContentType contentType) throws Exception {
        final HttpPost httpPost = new HttpPost(url.toURI());
        httpPost.setEntity(new StringEntity(content, contentType));
        return httpclient.execute(httpPost);
    }

}
