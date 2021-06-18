package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.db.CountryDao;
import org.railwaystations.rsapi.model.Country;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;

@RestController
public class CountriesResource {

    private final CountryDao countryDao;

    public CountriesResource(final CountryDao countryDao) {
        this.countryDao = countryDao;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/countries")
    public Collection<Country> list(@RequestParam("onlyActive") final Boolean onlyActive) {
        return countryDao.list(onlyActive == null || onlyActive);
    }

}
