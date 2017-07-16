package org.railwaystations.api.model;

@SuppressWarnings("PMD.LongVariable")
public class Country {

    private final String code;
    private final String name;
    private final String email;
    private final String twitterTags;
    private final String timetableUrlTemplate;

    public Country(final String code) {
        this(code, null, null, null, null);
    }

    public Country(final String code, final String name, final String email, final String twitterTags, final String timetableUrlTemplate) {
        this.code = code;
        this.name = name;
        this.email = email;
        this.twitterTags = twitterTags;
        this.timetableUrlTemplate = timetableUrlTemplate;
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

        return code != null ? code.equals(country.code) : country.code == null;
    }

    @Override
    public int hashCode() {
        return code != null ? code.hashCode() : 0;
    }
}
