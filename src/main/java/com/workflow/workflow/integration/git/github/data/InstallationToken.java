package com.workflow.workflow.integration.git.github.data;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object to represent a GitHub Rest api installation token.
 */
public class InstallationToken {
    private String token;
    private String expiresAt;

    public InstallationToken() {
    }

    public InstallationToken(String token, String expiresAt) {
        this.token = token;
        this.expiresAt = expiresAt;
    }

    @JsonProperty("expires_at")
    public String getExpiresAt() {
        return expiresAt;
    }

    @JsonProperty("expires_at")
    public void setExpiresAt(String expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    @JsonIgnore
    public Instant getExpiresInstant() {
        return Instant.parse(expiresAt);
    }
}
