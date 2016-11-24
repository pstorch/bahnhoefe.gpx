package github.pstorch.bahnhoefe.service;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class BahnhofTest {

    @Test
    public void distanceTo() throws Exception {
        assertEquals(53.1, new Bahnhof(0, "", 50.554550, 9.683787).distanceTo(50.196580, 9.189395), 0.1);
    }

    @Test
    public void appliesToPhotographer() {
        final Bahnhof bahnhof = new Bahnhof(0, "", 0.0, 0.0);
        assertThat(bahnhof.appliesTo(null, "test", null, null, null), is(false));
        assertThat(bahnhof.appliesTo(false, null, null, null, null), is(true));
        assertThat(bahnhof.appliesTo(true, null, null, null, null), is(false));
        bahnhof.setPhotographer("test");
        assertThat(bahnhof.appliesTo(null, "test", null, null, null), is(true));
        assertThat(bahnhof.appliesTo(false, null, null, null, null), is(false));
        assertThat(bahnhof.appliesTo(true, null, null, null, null), is(true));
    }

    @Test
    public void appliesToDistance() {
        final Bahnhof bahnhof = new Bahnhof(0, "", 50.554550, 9.683787);
        assertThat(bahnhof.appliesTo(null, null, 50, 50.8, 9.8), is(true));
        assertThat(bahnhof.appliesTo(null, null, 50, 55.0, 8.0), is(false));
    }

    @Test
    public void appliesToDistanceAndPhotographer() {
        final Bahnhof bahnhof = new Bahnhof(0, "", 50.554550, 9.683787);
        assertThat(bahnhof.appliesTo(null, "test", 50, 50.8, 9.8), is(false));
        bahnhof.setPhotographer("test");
        assertThat(bahnhof.appliesTo(null, "test", 50, 50.8, 9.8), is(true));
    }

}