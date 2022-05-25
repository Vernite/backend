package com.workflow.workflow.integration.git.github.entity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.integration.git.github.data.InstallationToken;
import com.workflow.workflow.user.User;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubInstallation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    long id;

    @JsonIgnore
    @Column(unique = true, length = 40)
    private String token;

    @JsonIgnore
    private Date expiresAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @JsonIgnore
    @Column(unique = true, nullable = false)
    private long installationId;

    @Column(unique = true, nullable = false, length = 40)
    private String gitHubUsername;

    private boolean suspended = false;

    public GitHubInstallation() {
    }

    public GitHubInstallation(long installationId, User user, String gitHubUsername) {
        this.installationId = installationId;
        this.user = user;
        this.expiresAt = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
        this.gitHubUsername = gitHubUsername;
    }

    public void updateToken(InstallationToken token) {
        this.token = token.getToken();
        this.expiresAt = Date.from(Instant.parse(token.getExpiresAt()));
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Date getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Date expiresAt) {
        this.expiresAt = expiresAt;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(long installationId) {
        this.installationId = installationId;
    }

    public String getGitHubUsername() {
        return gitHubUsername;
    }

    public void setGitHubUsername(String gitHubUsername) {
        this.gitHubUsername = gitHubUsername;
    }

    public boolean getSuspended() {
        return suspended;
    }

    public void setSuspended(boolean suspended) {
        this.suspended = suspended;
    }
}
