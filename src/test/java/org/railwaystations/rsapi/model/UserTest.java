package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;


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
        assertThat(new User(name, email, null, true, null, false, null, true).isValidForRegistration(), is(expected));
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
        assertThat(new User(name, email, license, photoOwner, link, false, null, true).isValid(), is(expected));
    }

    @Test
    public void testJsonDeserialization() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final User user = mapper.readerFor(User.class).readValue("{\"id\":\"1\", \"nickname\":\"@Nick Name\",\"email\":\"nick@example.com\",\"license\":\"CC0 1.0 Universell (CC0 1.0)\",\"photoOwner\":true,\"link\":\"https://example.com\",\"anonymous\":false,\"uploadToken\":\"token\",\"uploadTokenSalt\":\"123456\",\"key\":\"key\", \"admin\":true}");
        assertThat(user.getId(), is(0));
        assertThat(user.getName(), is("@Nick Name"));
        assertThat(user.getDisplayName(), is("@Nick Name"));
        assertThat(user.getNormalizedName(), is("nickname"));
        assertThat(user.getEmail(), is("nick@example.com"));
        assertThat(user.getLicense(), is("CC0 1.0 Universell (CC0 1.0)"));
        assertThat(user.isOwnPhotos(), is(true));
        assertThat(user.getUrl(), is("https://example.com"));
        assertThat(user.isAnonymous(), is(false));
        assertThat(user.getUploadToken(), nullValue());
        assertThat(user.getUploadTokenSalt(), nullValue());
        assertThat(user.getKey(), nullValue());
        assertThat(user.isAdmin(), is(false));
    }

    @Test
    public void testJsonSerialization() throws IOException {
        final ObjectMapper mapper = new ObjectMapper();
        final User user = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, 1234L, "key", true, null, true);
        final String json = mapper.writerFor(User.class).writeValueAsString(user);
        assertThat(json, is("{\"nickname\":\"@Nick Name\",\"email\":\"nick@example.com\",\"license\":\"CC0 1.0 Universell (CC0 1.0)\",\"photoOwner\":true,\"link\":\"https://example.com\",\"anonymous\":true,\"sendNotifications\":true,\"admin\":true,\"emailVerified\":false}"));
    }

    @Test
    public void testRoleUser() {
        final User user = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, 1234L, "key", false, null, true);
        final Set<String> roles = user.getRoles();
        assertThat(roles.size(), is(1));
        assertThat(roles.contains(User.ROLE_USER), is(true));
        assertThat(roles.contains(User.ROLE_ADMIN), is(false));
    }

    @Test
    public void testRolesAdmin() {
        final User admin = new User("@Nick Name", "https://example.com", "CC0 1.0 Universell (CC0 1.0)", 1, "nick@example.com", true, true, 1234L, "key", true, null, true);
        final Set<String> roles = admin.getRoles();
        assertThat(roles.size(), is(2));
        assertThat(roles.contains(User.ROLE_USER), is(true));
        assertThat(roles.contains(User.ROLE_ADMIN), is(true));
    }

}
