package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.api.StationsRepository;
import org.railwaystations.api.loader.StationLoader;
import org.railwaystations.api.model.Country;
import org.railwaystations.api.monitoring.LoggingMonitor;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CountriesResourceTest {

    @Test
    public void testList() throws IOException {
        final StationLoader loaderXY = Mockito.mock(StationLoader.class);
        Mockito.when(loaderXY.getCountry()).thenReturn(new Country("xy", "nameXY", "emailXY", "twitterXY", "timetableXY"));

        final StationLoader loaderAB = Mockito.mock(StationLoader.class);
        Mockito.when(loaderAB.getCountry()).thenReturn(new Country("ab", "nameAB", "emailAB", "twitterAB", "timetableAB"));

        final CountriesResource resource = new CountriesResource(new StationsRepository(new LoggingMonitor(), Arrays.asList(loaderAB, loaderXY), null, ""));

        final Set<Country> countries = resource.list();
        assertThat(countries.size(), equalTo(2));
        countries.stream().forEach(this::assertCountry);
    }

    @SuppressFBWarnings("DM_CONVERT_CASE")
    private void assertCountry(final Country country) {
        assertThat(country.getName(), equalTo("name" + country.getCode().toUpperCase()));
        assertThat(country.getEmail(), equalTo("email" + country.getCode().toUpperCase()));
        assertThat(country.getTwitterTags(), equalTo("twitter" + country.getCode().toUpperCase()));
        assertThat(country.getTimetableUrlTemplate(), equalTo("timetable" + country.getCode().toUpperCase()));
    }

}
