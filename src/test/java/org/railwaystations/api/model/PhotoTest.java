package org.railwaystations.api.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class PhotoTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    /**
     * Tests if the mapping of the known license names to the license URLs works as expected.
     */
    @ParameterizedTest
    @CsvSource({ "CC BY-NC 4.0 International, https://creativecommons.org/licenses/by-nc/4.0/",
        "CC BY-NC-SA 3.0 DE, https://creativecommons.org/licenses/by-nc-sa/3.0/de/",
        "CC BY-SA 4.0, https://creativecommons.org/licenses/by-sa/4.0/",
        "CC0 1.0 Universell (CC0 1.0), https://creativecommons.org/publicdomain/zero/1.0/" })
    public void license2LicenseUrlMapping(final String license, final String licenseUrl) {
        final Photo photo = new Photo(TEST_KEY, "url", new User("photographer", "photographerUrl", license), null, license);
        assertEquals(licenseUrl, photo.getLicenseUrl());
    }

    /**
     * Tests if the license URL is <code>null</code> for an unknown license.
     */
    @Test
    public void license2LicenseUrlMappingUnknownLicense() {
        final Photo photo = new Photo(TEST_KEY, "url", new User("photographer", "photographerUrl", null), null, "unknown license name");
        assertNull(photo.getLicenseUrl());
    }

}