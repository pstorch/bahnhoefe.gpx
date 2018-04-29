package org.railwaystations.api.batch;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.joda.time.DateTime;
import org.railwaystations.api.BackendHttpClient;

import java.io.File;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Locale;

public class DrupalFotoJsonMigration {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final BackendHttpClient httpclient;

    private final File targetDir;

    DrupalFotoJsonMigration(final String targetDirName) throws Exception {
        super();
        this.httpclient = new BackendHttpClient();
        this.targetDir = new File(targetDirName);
        FileUtils.forceMkdir(targetDir);
        final File fotoCopyScript = new File( targetDir,"foto-copy.sh");
        final PrintWriter fotoCopy = new PrintWriter(fotoCopyScript, "UTF-8");
        fotoCopy.println("#!/bin/bash");
        final String[] countries = new String[]{"AT", "BE", "CH", "CZ", "DE", "DE-IBNR", "DK", "ES", "FI", "FR", "HR", "HU", "NL", "NO", "PL", "UA", "UK"};
        for (final String country : countries) {
            migrate(country.toLowerCase(Locale.ENGLISH), fotoCopy);
        }
        fotoCopy.flush();
        fotoCopy.close();
    }

    private void migrate(final String country, final PrintWriter fotoCopy) throws Exception {
        fotoCopy.println("mkdir /var/www/drupal/fotos/" + country);

        final File target = new File(targetDir, "bahnhofsfotos-" + country + ".json");
        try (final PrintWriter out = new PrintWriter(target, "UTF-8")) {
            out.println("[");
            migratePhotos(out, "https://railway-stations.org/elastic/" + country + "/bahnhofsfotos.json", 0, country, fotoCopy);
            out.println("\n]");
            out.flush();
        }
    }

    private void migratePhotos(final PrintWriter out, final String url, final int page, final String country, final PrintWriter fotoCopy) throws Exception {
        final JsonNode tree = httpclient.readJsonFromUrl(new URL(url + "?page=" + page));
        for (int i = 0; i < tree.size(); i++) {
            if (i > 0 || page > 0) {
                out.println(",");
            }
            final JsonNode photoJson = tree.get(i);
            final int id = photoJson.get("BahnhofsID").asInt();
            final String photographer = photoJson.get("fotografenname").asText();
            final String flag = photoJson.get("flag").asText();
            final String bahnhofsfoto = photoJson.get("bahnhofsfoto").asText();
            final String fotoUrl = bahnhofsfoto.replace("/sites/default/files/previewbig/","/fotos/" + country + "/");
            final String license = photoJson.get("fotolizenz").asText().trim();
            final String erfasst = photoJson.get("erfasst").asText();
            final long createdAt = NumberUtils.isDigits(erfasst)?Long.parseLong(erfasst):DateTime.parse(erfasst).toDate().getTime();
            writePhoto(out, id, photographer, flag, fotoUrl, license, createdAt, country);
            fotoCopy.println("cp \"/var/www/drupal" + URLDecoder.decode(bahnhofsfoto, "UTF-8") + "\" \"/var/www/drupal" + URLDecoder.decode(fotoUrl, "UTF-8") + "\"");
        }
        if (tree.size() > 0) {
            migratePhotos(out, url, page + 1, country, fotoCopy);
        }
    }

    private void writePhoto(final PrintWriter out, final int id, final String photographer, final String flag, final String fotoUrl, final String license, final long createdAt, final String country) {
        final ObjectNode fotoJson = MAPPER.createObjectNode();
        fotoJson.put("BahnhofsID", id);
        fotoJson.put("bahnhofsfoto", fotoUrl);
        fotoJson.put("fotolizenz", license);
        fotoJson.put("fotografenname", photographer);
        fotoJson.put("erfasst", createdAt);
        fotoJson.put("flag", flag);
        fotoJson.put("laenderkennzeichen", country);
        out.print(fotoJson.toString());
    }

    public static void main(final String...args) throws Exception {
        new DrupalFotoJsonMigration("/tmp/drupal-migration");
    }
}
