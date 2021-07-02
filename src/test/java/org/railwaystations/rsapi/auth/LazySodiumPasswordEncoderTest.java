package org.railwaystations.rsapi.auth;

import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class LazySodiumPasswordEncoderTest {

    private final LazySodiumPasswordEncoder encoder = new LazySodiumPasswordEncoder();

    @Test
    public void testHashAndVerifyPassword() {
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final String key = encoder.encode(password);
        assertThat(encoder.matches(password, key), is(true));
    }

    @Test
    public void testHashAndVerifyChangedPassword() {
        final String password = RandomStringUtils.randomAlphanumeric(12);
        final String key = encoder.encode(password);
        assertThat(encoder.matches("something else", key), is(false));
    }

    @Test
    public void testExistingPassword() {
        final String key = "246172676F6E32696424763D3139246D3D36353533362C743D322C703D3124564A50666459365174574B786B6361745A2B37443241246D71324959726138695A564A6D5A2F2B53777A376868672B7659744341484861667A796A7469664A70426300000000000000000000000000000000000000000000000000000000000000";
        assertThat(encoder.matches("y89zFqkL6hro", key), is(true));
    }

}
