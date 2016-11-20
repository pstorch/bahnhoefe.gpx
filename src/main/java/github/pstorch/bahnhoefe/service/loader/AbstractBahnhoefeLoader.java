package github.pstorch.bahnhoefe.service.loader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import github.pstorch.bahnhoefe.service.Bahnhof;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public abstract class AbstractBahnhoefeLoader implements BahnhoefeLoader {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractBahnhoefeLoader.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();
    protected final Supplier<Map<Integer, Bahnhof>> cache;
    protected URL bahnhoefeUrl;
    protected URL photosUrl;

    protected AbstractBahnhoefeLoader(final URL photosUrl, final URL bahnhoefeUrl) {
        super();
        this.photosUrl = photosUrl;
        this.bahnhoefeUrl = bahnhoefeUrl;
        this.cache = Suppliers.memoizeWithExpiration(bahnhoefeSupplier(), 5, TimeUnit.MINUTES);
    }

    public final void setBahnhoefeUrl(final String bahnhoefeUrl) throws MalformedURLException {
        this.bahnhoefeUrl = new URL(bahnhoefeUrl);
    }

    public final void setPhotosUrl(final String photosUrl) throws MalformedURLException {
        this.photosUrl = new URL(photosUrl);
    }

    @Override
    public abstract String getCountryCode();

    @Override
    public Map<Integer, Bahnhof> loadBahnhoefe() {
        return cache.get();
    }

    private final Supplier<Map<Integer, Bahnhof>> bahnhoefeSupplier() {
        return () -> {
            LOG.info("Loading Bahnhoefe from bahnhoefe={}, photos={}", bahnhoefeUrl, photosUrl);
            Map<Integer, Bahnhof> bahnhoefe = null;
            try {
                bahnhoefe = loadAllBahnhoefe();
                loadPhotoFlags(bahnhoefe);
            } catch (final IOException e) {
                throw new RuntimeException("Unable to load all Bahnhoefe", e);
            }
            return bahnhoefe;
        };
    }

    protected abstract void loadPhotoFlags(final Map<Integer, Bahnhof> bahnhoefe) throws IOException;

    protected abstract Map<Integer, Bahnhof> loadAllBahnhoefe() throws IOException;

}
