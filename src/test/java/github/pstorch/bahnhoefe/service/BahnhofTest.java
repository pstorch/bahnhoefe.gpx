package github.pstorch.bahnhoefe.service;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BahnhofTest {

    @Test
    public void distanceTo() throws Exception {
        assertEquals(53.1, new Bahnhof(0, "", 50.554550, 9.683787).distanceTo(50.196580, 9.189395), 0.1);
    }

}