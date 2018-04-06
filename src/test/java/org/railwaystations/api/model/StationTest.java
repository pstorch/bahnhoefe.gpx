package org.railwaystations.api.model;

import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class StationTest {

    @Test
    public void distanceTo() throws Exception {
        assertEquals(53.1, new Station(0, "", "", new Coordinates(50.554550, 9.683787), null, null).distanceTo(50.196580, 9.189395), 0.1);
    }

    @Test
    public void appliesToNullPhotographer() {
        final Station bahnhof = new Station(0, "", "", new Coordinates(0.0, 0.0), null, null);
        assertThat(bahnhof.appliesTo(null, "test", null, null, null), is(false));
        assertThat(bahnhof.appliesTo(false, null, null, null, null), is(true));
        assertThat(bahnhof.appliesTo(true, null, null, null, null), is(false));
    }

    @Test
    public void appliesToPhotographer() {
        final Station bahnhof = new Station(0, "", "", new Coordinates(0.0, 0.0), new Photo(0, "URL", "test", "photographerUrl", null, "CC0", null));
        assertThat(bahnhof.appliesTo(null, "test", null, null, null), is(true));
        assertThat(bahnhof.appliesTo(false, null, null, null, null), is(false));
        assertThat(bahnhof.appliesTo(true, null, null, null, null), is(true));
    }

    @Test
    public void appliesToDistance() {
        final Station bahnhof = new Station(0, "", "", new Coordinates(50.554550, 9.683787), null, null);
        assertThat(bahnhof.appliesTo(null, null, 50, 50.8, 9.8), is(true));
        assertThat(bahnhof.appliesTo(null, null, 50, 55.0, 8.0), is(false));
    }

    @Test
    public void appliesToDistanceAndPhotographer() {
        final Station bahnhof = new Station(0, "", "", new Coordinates(50.554550, 9.683787), new Photo(0, "URL", "test", "photographerUrl", null, "CC0", null));
        assertThat(bahnhof.appliesTo(null, "test", 50, 50.8, 9.8), is(true));
    }

}