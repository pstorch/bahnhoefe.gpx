package org.railwaystations.api.resources;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.railwaystations.api.db.CountryDao;
import org.railwaystations.api.model.Country;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

public class CountriesResourceTest {

    @Test
    public void testList() {
        final CountryDao countryDao = Mockito.mock(CountryDao.class);
        final List<Country> countryList = new ArrayList<>();
        countryList.add(new Country("xy", "nameXY", "emailXY", "twitterXY", "timetableXY", "stationsXY", "photosXY"));
        countryList.add(new Country("ab", "nameAB", "emailAB", "twitterAB", "timetableAB", "stationsAB", "photosAB"));
        Mockito.when(countryDao.list()).thenReturn(countryList);

        final CountriesResource resource = new CountriesResource(countryDao);

        final Collection<Country> countries = resource.list();
        assertThat(countries.size(), equalTo(2));
        countries.stream().forEach(this::assertCountry);
    }

    @SuppressFBWarnings("DM_CONVERT_CASE")
    private void assertCountry(final Country country) {
        assertThat(country.getName(), equalTo("name" + country.getCode().toUpperCase()));
        assertThat(country.getEmail(), equalTo("email" + country.getCode().toUpperCase()));
        assertThat(country.getTwitterTags(), equalTo("twitter" + country.getCode().toUpperCase()));
        assertThat(country.getTimetableUrlTemplate(), equalTo("timetable" + country.getCode().toUpperCase()));
        assertThat(country.getStationsIndex(), equalTo("stations" + country.getCode().toUpperCase()));
        assertThat(country.getPhotosIndex(), equalTo("photos" + country.getCode().toUpperCase()));
    }

}
