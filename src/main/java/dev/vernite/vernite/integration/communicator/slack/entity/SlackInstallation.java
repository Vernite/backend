package dev.vernite.vernite.integration.communicator.slack.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class SlackInstallation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(unique = true, nullable = false, length = 100)
    private String token;

    private long installedAt;

    private String installerUserId;

    private String teamId;

    public SlackInstallation() {
    }

    public SlackInstallation(String token, long installedAt, String installerUserId, String teamId) {
        this.token = token;
        this.installedAt = installedAt;
        this.installerUserId = installerUserId;
        this.teamId = teamId;
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

    public long getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(long installedAt) {
        this.installedAt = installedAt;
    }

    public String getInstallerUserId() {
        return installerUserId;
    }

    public void setInstallerUserId(String installerUserId) {
        this.installerUserId = installerUserId;
    }

    public String getTeamId() {
        return teamId;
    }

    public void setTeamId(String teamId) {
        this.teamId = teamId;
    }
}
