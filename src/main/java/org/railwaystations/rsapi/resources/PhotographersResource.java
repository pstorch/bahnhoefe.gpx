package org.railwaystations.rsapi.resources;

import org.railwaystations.rsapi.StationsRepository;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class PhotographersResource {

    private static final String COUNTRY = "country";

    private final StationsRepository repository;

    public PhotographersResource(final StationsRepository repository) {
        this.repository = repository;
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/photographers")
    public Map<String, Long> get(@RequestParam(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/photographers.txt")
    public Map<String, Long> getAsText(@RequestParam(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/photographers.json")
    public Map<String, Long> getAsJson(@RequestParam(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return getWithCountry(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8", MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers")
    public Map<String, Long> getWithCountry(@PathVariable(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return repository.getPhotographerMap(country);
    }

    @GetMapping(produces = {MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.txt")
    public Map<String, Long> getWithCountryAsText(@PathVariable(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return repository.getPhotographerMap(country);
    }

    @GetMapping(produces = {MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8"}, value = "/{country}/photographers.json")
    public Map<String, Long> getWithCountryAsJson(@PathVariable(value = PhotographersResource.COUNTRY, required = false) final String country) {
        return repository.getPhotographerMap(country);
    }

}
