package com.workflow.workflow.integration.git.github;

import java.sql.Timestamp;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

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
    private long installationId;

    public GitHubInstallation() {
    }

    public GitHubInstallation(long installationId, User user) {
        this.installationId = installationId;
        this.user = user;
        this.expiresAt = new Timestamp(0);
    }

    public Timestamp getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Timestamp expiresAt) {
        this.expiresAt = expiresAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getInstallationId() {
        return installationId;
    }

    public void setInstallationId(long installationId) {
        this.installationId = installationId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public void update(InstallationToken token) {
        this.token = token.getToken();
        this.expiresAt = new Timestamp(token.getExpiresInstant().toEpochMilli());
    }
}
