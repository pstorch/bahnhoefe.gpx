package org.railwaystations.rsapi.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.internal.constraintvalidators.hv.EmailValidator;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class User {

    private static final Map<String, String> LICENSE_MAP = new HashMap<>(2);

    /** the one and only valid license */
    public static final String CC0 = "CC0 1.0 Universell (CC0 1.0)";

    public static final String EMAIL_VERIFIED = "VERIFIED";
    public static final String EMAIL_VERIFIED_AT_NEXT_LOGIN = "NEXT_LOGIN";
    public static final String EMAIL_VERIFICATION_TOKEN = "TOKEN:";
    public static final String ROLE_ADMIN = "ROLE_ADMIN";
    public static final String ROLE_USER = "ROLE_USER";

    static {
        LICENSE_MAP.put("CC0", CC0);
        LICENSE_MAP.put("CC4", "CC BY-SA 4.0");
    }

    @JsonProperty("nickname")
    private final String name;

    @JsonProperty("link")
    private final String url;

    @JsonProperty("license")
    private final String license;

    @JsonIgnore
    private int id;

    @JsonIgnore
    private String email;

    @JsonIgnore
    private String normalizedName;

    @JsonProperty("photoOwner")
    private boolean ownPhotos;

    @JsonProperty("anonymous")
    private boolean anonymous;

    @JsonProperty(value = "uploadToken", access = JsonProperty.Access.READ_ONLY)
    private String uploadToken;

    @JsonIgnore
    private Long uploadTokenSalt;

    @JsonIgnore
    private String key;

    @JsonProperty(value = "admin", access = JsonProperty.Access.READ_ONLY)
    private final boolean admin;

    @JsonIgnore
    private String emailVerification;

    @JsonIgnore
    private String emailVerificationToken;

    @JsonProperty
    private String newPassword;

    @JsonProperty
    private boolean sendNotifications;

    public User(final String name, final String url, final String license, final int id, final String email,
                final boolean ownPhotos, final boolean anonymous, final Long uploadTokenSalt,
                final String key, final boolean admin, final String emailVerification, final boolean sendNotifications) {
        this.name = name;
        this.url = url;
        this.license = license;
        this.id = id;
        this.email = normalizeEmail(email);
        this.normalizedName = normalizeName(name);
        this.ownPhotos = ownPhotos;
        this.anonymous = anonymous;
        this.uploadTokenSalt = uploadTokenSalt;
        this.key = key;
        this.admin = admin;
        this.emailVerification = emailVerification;
        this.sendNotifications = sendNotifications;
    }

    /**
     * Constructor for Registration
     */
    public User(@JsonProperty("nickname") final String name,
                        @JsonProperty("email") final String email,
                        @JsonProperty("license") final String license,
                        @JsonProperty("photoOwner") final boolean photoOwner,
                        @JsonProperty("link") final String link,
                        @JsonProperty("anonymous") final boolean anonymous,
                        @JsonProperty("newPassword") final String newPassword,
                        @JsonProperty("sendNotifications") final Boolean sendNotifications) {
        this.name = StringUtils.trimToEmpty(name);
        this.normalizedName = normalizeName(name);
        this.email = normalizeEmail(email);
        this.license = LICENSE_MAP.getOrDefault(license, license);
        this.ownPhotos = photoOwner;
        this.anonymous = anonymous;
        this.url = StringUtils.trimToEmpty(link);
        this.admin = false;
        this.emailVerification = null;
        this.newPassword = newPassword;
        this.sendNotifications = sendNotifications == null || sendNotifications;
    }

    public static Map<String, User> toNameMap(final List<User> list) {
        return list.stream().collect(Collectors.toMap (User::getNormalizedName, i -> i));
    }

    public static Map<Integer, User> toIdMap(final List<User> list) {
        return list.stream().collect(Collectors.toMap (User::getId, i -> i));
    }

    public static String normalizeName(final String name) {
        return StringUtils.trimToEmpty(name).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]","");
    }

    public static String normalizeEmail(final String email) {
        return StringUtils.trimToEmpty(email).toLowerCase(Locale.ENGLISH);
    }

    @JsonProperty("nickname")
    public String getName() {
        return name;
    }

    @JsonProperty("link")
    public String getUrl() {
        return url;
    }

    @JsonIgnore
    public String getDisplayUrl() {
        return anonymous || StringUtils.isBlank(url) ? "https://railway-stations.org" : url;
    }

    public String getLicense() {
        return license;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final User that = (User) o;

        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
    }

    public int getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNormalizedName() {
        return normalizedName;
    }

    @JsonProperty("photoOwner")
    public boolean isOwnPhotos() {
        return ownPhotos;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public Long getUploadTokenSalt() {
        return uploadTokenSalt;
    }

    @JsonIgnore
    public String getDisplayName() {
        return anonymous ? "Anonym" : getName();
    }

    public void setUploadTokenSalt(final Long uploadTokenSalt) {
        this.uploadTokenSalt = uploadTokenSalt;
    }

    public String getUploadToken() {
        return uploadToken;
    }

    public void setUploadToken(final String uploadToken) {
        this.uploadToken = uploadToken;
    }

    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Checks if we have got a name and valid email for registration.
     */
    @JsonIgnore
    public boolean isValidForRegistration() {
        return StringUtils.isNotBlank(name) &&
                StringUtils.isNotBlank(email) &&
                new EmailValidator().isValid(email, null);
    }

    @JsonIgnore
    public boolean isValid() {
        if (!isValidForRegistration()) {
            return false;
        }
        if (StringUtils.isNotBlank(url)) {
            final URL validatedUrl;
            try {
                validatedUrl = new URL( url );
            } catch (final MalformedURLException e) {
                return false;
            }
            if (!validatedUrl.getProtocol().matches("https?")) {
                return false;
            }
        }

        if (!ownPhotos) {
            return false;
        }

        return CC0.equals(license);
    }

    @Override
    public String toString() {
        return "User{" +
                "name='" + name + '\'' +
                ", url='" + url + '\'' +
                ", license='" + license + '\'' +
                ", email='" + email + '\'' +
                ", ownPhotos=" + ownPhotos +
                ", anonymous=" + anonymous +
                '}';
    }

    public String getKey() {
        return this.key;
    }

    public void setKey(final String key) {
        this.key = key;
    }

    public boolean isAdmin() {
        return admin;
    }

    @JsonProperty("emailVerified")
    public boolean isEmailVerified() {
        return EMAIL_VERIFIED.equals(emailVerification);
    }

    @JsonIgnore
    public boolean isEmailVerifiedWithNextLogin() {
        return EMAIL_VERIFIED_AT_NEXT_LOGIN.equals(emailVerification);
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(final String newPassword) {
        this.newPassword = newPassword;
    }

    public String getEmailVerification() {
        return emailVerification;
    }

    public void setEmailVerification(final String emailVerification) {
        this.emailVerification = emailVerification;
    }

    public String getEmailVerificationToken() {
        return emailVerificationToken;
    }

    public void setEmailVerificationToken(final String emailVerificationToken) {
        this.emailVerificationToken = emailVerificationToken;
        this.emailVerification = EMAIL_VERIFICATION_TOKEN + emailVerificationToken;
    }

    public boolean isSendNotifications() {
        return sendNotifications;
    }

    public void setSendNotifications(final boolean sendNotifications) {
        this.sendNotifications = sendNotifications;
    }

    @JsonIgnore
    public Set<String> getRoles() {
        final HashSet<String> roles = new HashSet<>();
        roles.add(ROLE_USER);
        if (isAdmin()) {
            roles.add(ROLE_ADMIN);
        }
        return roles;
    }
}
