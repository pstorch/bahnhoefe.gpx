package org.railwaystations.api.loader;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.apache.commons.lang3.StringUtils;
import org.railwaystations.api.BackendHttpClient;
import org.railwaystations.api.model.Photographer;
import org.railwaystations.api.model.elastic.Fotograf;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class PhotographerLoader {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String HITS_ELEMENT = "hits";

    private final LoadingCache<String, Map<String, Photographer>> cache;

    public PhotographerLoader(final URL photographersUrl) {
        super();
        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(60, TimeUnit.MINUTES).build(
                new PhotographerCacheLoader(photographersUrl));
    }

    public final Map<String, Photographer> loadPhotographers() {
        return cache.getUnchecked("all");
    }

    public void refresh() {
        cache.invalidateAll();
    }

    private static class PhotographerCacheLoader extends CacheLoader<String, Map<String, Photographer>> {

        private final URL photographersUrl;

        private final BackendHttpClient httpclient;

        private PhotographerCacheLoader(final URL photographersUrl) {
            this.photographersUrl = photographersUrl;
            this.httpclient = new BackendHttpClient();
        }

        public Map<String, Photographer> load(final String key) {
            final Map<String, Photographer> photographers = new HashMap<>();
            try {
                final JsonNode hits = httpclient.readJsonFromUrl(photographersUrl)
                        .get(PhotographerLoader.HITS_ELEMENT)
                        .get(PhotographerLoader.HITS_ELEMENT);
                for (int i = 0; i < hits.size(); i++) {
                    final JsonNode sourceJson = hits.get(i).get("_source");
                    final Photographer photographer = createPhotographerFromElastic(sourceJson);
                    photographers.put(photographer.getName(), photographer);
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
            return photographers;
        }

        private Photographer createPhotographerFromElastic(final JsonNode sourceJson) throws JsonProcessingException {
            final Fotograf fotograf = MAPPER.treeToValue(sourceJson, Fotograf.class);
            return new Photographer(fotograf.getName(), fotograf.getUrl(), StringUtils.trimToEmpty(fotograf.getLicense()));
        }

    }

}
