package github.pstorch.bahnhoefe.gpx;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class BahnhoefeLoader {
	
	private static final ObjectMapper MAPPER = new ObjectMapper();
	
	private static final String TITLE_ELEMENT = "title";

	private static final String LON_ELEMENT = "lon";

	private static final String LAT_ELEMENT = "lat";

	private static final String HITS_ELEMENT = "hits";

	public URL bahnhoefeUrl;
	public URL photosUrl;

	public BahnhoefeLoader(final String bahnhoefeUrl, final String photosUrl) throws MalformedURLException {
		this.bahnhoefeUrl = new URL(bahnhoefeUrl);
		this.photosUrl = new URL(photosUrl);
	}
	
	public Map<Integer, Bahnhof> loadBahnhoefe() throws MalformedURLException, IOException, JsonProcessingException {
		final Map<Integer, Bahnhof> bahnhoefe = loadAllBahnhoefe();
		loadPhotoFlags(bahnhoefe);
		return bahnhoefe;
	}

	private void loadPhotoFlags(final Map<Integer, Bahnhof> bahnhoefe) throws IOException, JsonProcessingException {
		try (InputStream is = photosUrl.openStream()) {
			final JsonNode tree = BahnhoefeLoader.MAPPER.readTree(is);
			for (int i = 0; i < tree.size(); i++) {
				final JsonNode bahnhofPhoto = tree.get(i);
				final int bahnhofsNr = bahnhofPhoto.get("bahnhofsnr").asInt();
				final Bahnhof bahnhof = bahnhoefe.get(bahnhofsNr);
				if (bahnhof != null) {
					bahnhof.setHasPhoto(true);
				}
			}
		}
	}

	@SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
	private Map<Integer, Bahnhof> loadAllBahnhoefe() throws IOException, JsonProcessingException {
		final Map<Integer, Bahnhof> bahnhoefe = new HashMap<>();

		try (InputStream is = bahnhoefeUrl.openStream()) {
			final JsonNode tree = BahnhoefeLoader.MAPPER.readTree(is);
			final JsonNode hits = tree.get(BahnhoefeLoader.HITS_ELEMENT).get(BahnhoefeLoader.HITS_ELEMENT);
			for (int i = 0; i < hits.size(); i++) {
				final JsonNode hit = hits.get(i);
				final JsonNode bahnhofJson = hit.get("_source");
				final Bahnhof bahnhof = new Bahnhof(bahnhofJson.get("BahnhofNr").asInt(), bahnhofJson.get(BahnhoefeLoader.TITLE_ELEMENT).asText(),
						bahnhofJson.get(BahnhoefeLoader.LAT_ELEMENT).asDouble(), bahnhofJson.get(BahnhoefeLoader.LON_ELEMENT).asDouble());
				bahnhoefe.put(bahnhof.getId(), bahnhof);
			}
		}
		return bahnhoefe;
	}
	
}
