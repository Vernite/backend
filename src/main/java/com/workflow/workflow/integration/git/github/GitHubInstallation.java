package com.workflow.workflow.integration.git.github;

import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.integration.git.github.service.InstallationToken;
import com.workflow.workflow.user.User;

@Entity
public class GitHubInstallation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;
    private String token;
    private Timestamp expiresAt;
    @ManyToOne
    private User user;
    @Column(unique = true)
    private long installationId;
    private String gitHubUsername;

    public GitHubInstallation() {
    }

    public GitHubInstallation(long installationId, User user, String gitHubUsername) {
        this.installationId = installationId;
        this.user = user;
        this.expiresAt = new Timestamp(0);
        this.gitHubUsername = gitHubUsername;
    }

    @JsonIgnore
    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    @JsonIgnore
    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonIgnore
    public long getInstallationId() {
        return installationId;
    }

    @JsonIgnore
    public void setInstallationId(long installationId) {
        this.installationId = installationId;
    }

    @JsonIgnore
    public String getToken() {
        return token;
    }

    @JsonIgnore
    public void setToken(String token) {
        this.token = token;
    }

    @JsonIgnore
    public User getUser() {
        return user;
    }

    @JsonIgnore
    public void setUser(User user) {
        this.user = user;
    }

    public String getGitHubUsername() {
        return gitHubUsername;
    }

    public void setGitHubUsername(String gitHubUsername) {
        this.gitHubUsername = gitHubUsername;
    }

    public void update(InstallationToken token) {
        this.token = token.getToken();
        this.expiresAt = new Timestamp(token.getExpiresInstant().toEpochMilli());
    }
}
