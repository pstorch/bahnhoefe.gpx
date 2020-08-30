package org.railwaystations.api.model;

import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class StationTest {

    private static final Station.Key TEST_KEY = new Station.Key("", "0");

    @Test
    public void distanceTo() {
        assertEquals(53.1, new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null, true).distanceTo(50.196580, 9.189395), 0.1);
    }

    @Test
    public void appliesToNullPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), null, null, true);
        assertThat(station.appliesTo(null, "test", null, null, null, null), is(false));
        assertThat(station.appliesTo(false, null, null, null, null, null), is(true));
        assertThat(station.appliesTo(true, null, null, null, null, null), is(false));
    }

    @Test
    public void appliesToPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(0.0, 0.0), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", null, null, null, null), is(true));
        assertThat(station.appliesTo(false, null, null, null, null, null), is(false));
        assertThat(station.appliesTo(true, null, null, null, null, null), is(true));
    }

    @Test
    public void appliesToDistance() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), null, null, true);
        assertThat(station.appliesTo(null, null, 50, 50.8, 9.8, null), is(true));
        assertThat(station.appliesTo(null, null, 50, 55.0, 8.0, null), is(false));
    }

    @Test
    public void appliesToDistanceAndPhotographer() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", 50, 50.8, 9.8, null), is(true));
    }

    @Test
    public void appliesToActive() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), true);
        assertThat(station.appliesTo(null, "test", null, null, null, true), is(true));
    }

    @Test
    public void appliesToInactive() {
        final Station station = new Station(TEST_KEY, "", new Coordinates(50.554550, 9.683787), new Photo(TEST_KEY, "URL", createTestPhotographer(), null, "CC0"), false);
        assertThat(station.appliesTo(null, "test", null, null, null, false), is(true));
    }

    private User createTestPhotographer() {
        return new User("test", "photographerUrl", "CC0", 0, null, true, false, null, null, false, null);
    }
}