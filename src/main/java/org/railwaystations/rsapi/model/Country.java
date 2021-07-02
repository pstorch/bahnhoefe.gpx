package org.railwaystations.rsapi.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@SuppressWarnings("PMD.LongVariable")
public class Country {

    private String code;
    private String name;
    private String email = "bahnhofsfotos@deutschlands-bahnhoefe.de";
    private String twitterTags = "@android_oma, #dbHackathon, #dbOpendata, #Bahnhofsfoto, @khgdrn";
    private String timetableUrlTemplate;
    private String overrideLicense;
    private boolean active;
    private final List<ProviderApp> providerApps = new ArrayList<>();

    public Country() {
        super();
    }

    public Country(final String code) {
        this(code, null, null, null, null, null, true);
    }

    public Country(final String code, final String name, final String email, final String twitterTags,
                   final String timetableUrlTemplate, final String overrideLicense, final boolean active) {
        this.code = code;
        this.name = name;
        this.email = email != null ? email : this.email;
        this.twitterTags = twitterTags != null ? twitterTags : this.twitterTags;
        this.timetableUrlTemplate = timetableUrlTemplate;
        this.overrideLicense = overrideLicense;
        this.active = active;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getTwitterTags() {
        return twitterTags;
    }

    public String getTimetableUrlTemplate() {
        return timetableUrlTemplate;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final Country country = (Country) o;

        return Objects.equals(code, country.code);
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }

    public String getOverrideLicense() {
        return overrideLicense;
    }

    public List<ProviderApp> getProviderApps() {
        return providerApps;
    }

    public boolean isActive() {
        return active;
    }
}
