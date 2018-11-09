package org.railwaystations.api.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class StationTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    @Test
    public void distanceTo() {
        assertEquals(53.1, new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null).distanceTo(50.196580, 9.189395), 0.1);
    }

    @Test
    public void appliesToNullPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), null, null);
        assertThat(station.appliesTo(null, "test", null, null, null), is(false));
        assertThat(station.appliesTo(false, null, null, null, null), is(true));
        assertThat(station.appliesTo(true, null, null, null, null), is(false));
    }

    @Test
    public void appliesToPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), new Photo(TEST_KEY, "URL", new User("test", "photographerUrl", "CC0"), null, "CC0"));
        assertThat(station.appliesTo(null, "test", null, null, null), is(true));
        assertThat(station.appliesTo(false, null, null, null, null), is(false));
        assertThat(station.appliesTo(true, null, null, null, null), is(true));
    }

    @Test
    public void appliesToDistance() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null);
        assertThat(station.appliesTo(null, null, 50, 50.8, 9.8), is(true));
        assertThat(station.appliesTo(null, null, 50, 55.0, 8.0), is(false));
    }

    @Test
    public void appliesToDistanceAndPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", new User("test", "photographerUrl", "CC0"), null, "CC0"));
        assertThat(station.appliesTo(null, "test", 50, 50.8, 9.8), is(true));
    }

}