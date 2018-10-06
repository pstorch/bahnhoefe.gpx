package org.railwaystations.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.validator.constraints.Email;
import org.hibernate.validator.constraints.NotEmpty;

public class Registration {

    @NotEmpty
    private final String nickname;

    @NotEmpty @Email
    private final String email;

    @NotEmpty
    private final String license;

    private final boolean photoOwner;

    private final String link;

    public Registration(@JsonProperty("nickname") final String nickname,
                        @JsonProperty("email") final String email,
                        @JsonProperty("license") final String license,
                        @JsonProperty("photoOwner") final boolean photoOwner,
                        @JsonProperty("link") final String link) {
        this.nickname = StringUtils.trimToEmpty(nickname);
        this.email = StringUtils.trimToEmpty(email);
        this.license = license;
        this.photoOwner = photoOwner;
        this.link = StringUtils.trimToEmpty(link);
    }

    @JsonProperty("nickname")
    public String getNickname() {
        return nickname;
    }

    @JsonProperty("email")
    public String getEmail() {
        return email;
    }

    @JsonProperty("license")
    public String getLicense() {
        return license;
    }

    @JsonProperty("photoOwner")
    public boolean isPhotoOwner() {
        return photoOwner;
    }

    @JsonProperty("link")
    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "Registration{" +
                "nickname='" + nickname + '\'' +
                ", email='" + email + '\'' +
                ", license='" + license + '\'' +
                ", photoOwner=" + photoOwner +
                ", link='" + link + '\'' +
                '}';
    }
}
