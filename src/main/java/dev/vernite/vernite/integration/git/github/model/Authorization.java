/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.integration.git.github.model;

import java.time.Instant;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vernite.vernite.integration.git.github.api.model.GitHubUser;
import dev.vernite.vernite.integration.git.github.api.model.OauthToken;
import dev.vernite.vernite.user.User;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * Entity for representing GitHub authorization.
 */
@Data
@Schema(name = "GitHubAuthorization")
@Entity(name = "github_authorization")
public class Authorization {

    @Id
    @Positive
    private long id;

    @NotBlank
    @Column(nullable = false)
    private String login;

    @NotBlank
    @Column(nullable = false)
    private String avatarUrl;

    @NotBlank
    @JsonIgnore
    @Column(unique = true, nullable = false)
    private String accessToken;

    @NotNull
    @JsonIgnore
    @Column(nullable = false)
    private Date expires;

    @NotBlank
    @JsonIgnore
    @Column(nullable = false)
    private String refreshToken;

    @NotNull
    @JsonIgnore
    @Column(nullable = false)
    private Date refreshTokenExpires;

    @NotBlank
    @JsonIgnore
    @Column(nullable = false)
    private String tokenType;

    @NotNull
    @JsonIgnore
    @Column(nullable = false)
    private String scope;

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    private User user;

    /**
     * Updates authorization with new data.
     * 
     * @param token      OAuth token
     * @param user       GitHub user
     * @param userEntity user entity
     */
    public void update(OauthToken token, GitHubUser user, User userEntity) {
        refreshToken(token);
        if (getId() != 0) {
            setId(user.getId());
        }
        this.login = user.getLogin();
        this.avatarUrl = user.getAvatarUrl();
        if (getUser() != null) {
            setUser(userEntity);
        }
    }

    /**
     * Refresh token.
     * 
     * @param token Oauth token
     */
    public void refreshToken(OauthToken token) {
        setAccessToken(token.getAccessToken());
        setExpires(Date.from(Instant.now().plusSeconds(token.getExpiresIn())));
        setRefreshToken(token.getRefreshToken());
        setRefreshTokenExpires(Date.from(Instant.now().plusSeconds(token.getRefreshTokenExpiresIn())));
        setTokenType(token.getTokenType());
        setScope(token.getScope());
    }

    /**
     * @return true if token should be refreshed
     */
    public boolean shouldRefreshToken() {
        return Instant.now().isAfter(getExpires().toInstant());
    }

}
