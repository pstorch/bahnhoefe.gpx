package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.beans.ConstructorProperties;
import java.util.Objects;

public class Station {

    private static final int EARTH_RADIUS = 6371;

    @JsonUnwrapped
    private final Key key;

    @JsonProperty
    private String title;

    @JsonUnwrapped
    private Coordinates coordinates;

    @JsonIgnore
    private int photographerId;

    @JsonProperty
    private String photographer;

    @JsonProperty
    private String photographerUrl;

    @JsonProperty("DS100")
    private final String ds100;

    @JsonProperty
    private String photoUrl;

    @JsonProperty
    private String license;

    @JsonProperty
    private String licenseUrl;

    @JsonProperty
    private Long createdAt;

    @JsonProperty
    private boolean active;

    public Station() {
        this(new Key("", "0"), null, new Coordinates(0.0, 0.0), null, true);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final Photo photo, final boolean active) {
        this(key, title, coordinates, null, photo, active);
    }

    public Station(final Key key, final String title, final Coordinates coordinates, final String ds100, final Photo photo, final boolean active) {
        super();
        this.key = key;
        this.title = title;
        this.coordinates = coordinates;
        this.ds100 = ds100;
        this.active = active;
        setPhoto(photo);
    }

    public void setPhoto(final Photo photo) {
        if (photo != null) {
            final User user = photo.getPhotographer();
            if (user != null) {
                this.photographerId = user.getId();
                this.photographer = user.getDisplayName();
                this.photographerUrl = user.getDisplayUrl();
            } else {
                this.photographerId = 0;
                this.photographer = "-";
                this.photographerUrl = "";
            }

            this.photoUrl = photo.getUrl();
            this.license = photo.getLicense();
            this.licenseUrl = photo.getLicenseUrl();
            this.photographerUrl = photo.getPhotographer().getDisplayUrl();
            this.createdAt = photo.getCreatedAt();
        } else {
            this.photographerId = 0;
            this.photographer = null;
            this.photoUrl = null;
            this.license = null;
            this.licenseUrl = null;
            this.photographerUrl = null;
            this.createdAt = null;
        }
    }

    public Key getKey() {
        return this.key;
    }

    public String getTitle() {
        return this.title;
    }

    public Coordinates getCoordinates() {
        return this.coordinates;
    }

    public boolean hasPhoto() {
        return this.photographer != null;
    }

    public String getPhotographer() { return this.photographer; }

    /*
     * Calculate distance in km between this objects position and the given latitude and longitude.
     * Uses Haversine method as its base.
     *
     * @returns Distance in km
     */
    public double distanceTo(final double latitude, final double longitude) {
        final double latDistance = Math.toRadians(latitude - this.coordinates.getLat());
        final double lonDistance = Math.toRadians(longitude - this.coordinates.getLon());
        final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(this.coordinates.getLat())) * Math.cos(Math.toRadians(latitude))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return Station.EARTH_RADIUS * c;
    }

    public boolean appliesTo(final Boolean hasPhoto, final String photographer, final Integer maxDistance, final Double lat, final Double lon, final Boolean active) {
        boolean result = true;
        if (hasPhoto != null) {
            result = this.hasPhoto() == hasPhoto;
        }
        if (photographer != null) {
            result &= photographer.equals(this.getPhotographer());
        }
        if (maxDistance != null && lat != null && lon != null) {
            result &= this.distanceTo(lat, lon) < maxDistance;
        }
        if (active != null) {
            result &= active == this.active;
        }
        return result;
    }

    public String getDS100() {
        return ds100;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getLicense() {
        return license;
    }

    public String getLicenseUrl() {
        return licenseUrl;
    }

    public String getPhotographerUrl() {
        return photographerUrl;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public boolean isActive() {
        return active;
    }

    public int getPhotographerId() {
        return photographerId;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setActive(final Boolean active) {
        this.active = active;
    }

    public void setCoordinates(final Coordinates coordinates) {
        this.coordinates = coordinates;
    }

    @SuppressWarnings("PMD.ShortClassName")
    public static final class Key {
        @JsonProperty
        private final String country;

        @JsonProperty("idStr")
        private final String id;

        public Key() {
            this("","");
        }

        @ConstructorProperties({"country", "id"})
        public Key(final String country, final String id) {
            this.country = country;
            this.id = id;
        }

        public String getCountry() {
            return country;
        }

        public String getId() {
            return id;
        }

        @JsonGetter("id")
        public int getIdLegacy() {
            try {
                return Integer.parseInt(id);
            } catch (final NumberFormatException ignored) {
                return -1;
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof  Key)) {
                return false;
            }
            final Key key = (Key) o;
            return Objects.equals(country, key.country) &&
                    Objects.equals(id, key.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(country, id);
        }

        @Override
        public String toString() {
            return "Key{" +
                    "country='" + country + '\'' +
                    ", id='" + id + '\'' +
                    '}';
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof  Station)) {
            return false;
        }
        final Station other = (Station) o;
        return Objects.equals(key, other.getKey());
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

}
