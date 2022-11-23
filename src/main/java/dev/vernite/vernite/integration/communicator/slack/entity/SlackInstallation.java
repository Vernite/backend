package dev.vernite.vernite.integration.communicator.slack.entity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vernite.vernite.user.User;

@Entity
public class SlackInstallation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JsonIgnore
    @Column(unique = true, nullable = false, length = 100)
    private String token;

    private String installerUserId;

    @JsonIgnore
    private String teamId;

    private String teamName;

    @JsonIgnore
    @ManyToOne(optional = false)
    private User user;

    public SlackInstallation() {
    }

    public SlackInstallation(String token, String installerUserId, String teamId, String teamName, User user) {
        this.token = token;
        this.installerUserId = installerUserId;
        this.teamId = teamId;
        this.teamName = teamName;
        this.user = user;
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

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getTeamName() {
        return teamName;
    }

    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
}
