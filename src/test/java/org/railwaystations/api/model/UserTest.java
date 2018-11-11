package org.railwaystations.api.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;


public class UserTest {

    @ParameterizedTest
    @CsvSource({ "nickname, email@example.com, true",
            "nickname, email@example., false",
            ", email@example.com, false",
            "'', email@example.com, false",
            "nickname, email.example.com, false",
            "nickname,, false",
            "nickname,' ', false"})
    public void testIsValidForRegistration(final String name, final String email, final boolean expected) {
        assertThat(new User(name, email, null, true, null, false).isValidForRegistration(), is(expected));
    }

    @ParameterizedTest
    @CsvSource({ "nickname, email@example.com, CC0, true,                    , true",
                 "nickname, email@example.com, CC0 1.0 Universell (CC0 1.0), true, , true",
                 "nickname, email@example.com, CC0, true, http://example.com , true",
                 "nickname, email@example.com, CC0, true, https://example.com, true",
                 "nickname, email@example.com, CC0, true, ftp://example.com  , false",
                 "nickname, email@example.com, CC0, true, email@example.com  , false",
                 "nickname, email@example.com, CC0, true, '                 ', true",
                 "nickname, email@example.com, CC0, false,                   , false",
                 "nickname, email@example.com, CC4, true,                    , false",
                 "        , email@example.com, CC0, false,                   , false"})
    public void testIsValid(final String name, final String email, final String license, final boolean photoOwner, final String link, final boolean expected) {
        assertThat(new User(name, email, license, photoOwner, link, false).isValid(), is(expected));
    }

}
