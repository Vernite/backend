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

import dev.vernite.vernite.common.constraints.NullOrNotBlank;
import dev.vernite.vernite.integration.git.github.api.model.AppToken;
import dev.vernite.vernite.integration.git.github.api.model.GitHubInstallation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * Entity for representing GitHub installation.
 */
@Data
@Entity(name = "github_installation")
public class Installation {

    @Id
    @Positive
    private long id;

    @NullOrNotBlank
    @Column(unique = true)
    private String token;

    @NotNull
    @Column(nullable = false)
    private Date expires;

    @NotBlank
    @Column(nullable = false)
    private String targetType;

    private boolean suspended = false;

    /**
     * Updates installation with new data.
     * 
     * @param installation response from GitHub api
     */
    public void update(GitHubInstallation installation) {
        if (getId() == 0) {
            setId(installation.getId());
        }
        if (getExpires() == null) {
            setExpires(new Date(1));
        }
        setTargetType(installation.getTargetType());
        setSuspended(installation.getSuspendedAt() != null);
    }

    /**
     * Refreshes token with response from GitHub.
     * 
     * @param appToken response from GitHub
     */
    public void refreshToken(AppToken appToken) {
        setToken(appToken.getToken());
        setExpires(Date.from(Instant.parse(appToken.getExpiresAt())));
    }

    /**
     * @return true if token should be refreshed
     */
    public boolean shouldRefreshToken() {
        return Instant.now().isAfter(getExpires().toInstant());
    }

}
