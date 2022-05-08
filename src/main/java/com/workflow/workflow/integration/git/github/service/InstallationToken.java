package com.workflow.workflow.integration.git.github.service;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InstallationToken {
    private String token;
    private String expiresAt;

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

    public Instant getExpiresInstant() {
        return Instant.parse(expiresAt);
    }
}