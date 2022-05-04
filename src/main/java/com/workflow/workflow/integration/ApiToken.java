package com.workflow.workflow.integration;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.workflow.workflow.user.User;

@Entity
public class ApiToken {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String token;
    private String refreshToken;
    private String apiType;
    @ManyToOne
    private User user;

    public ApiToken() {}

    public ApiToken(String apiToken, String refreshToken, String apiType, User user) {
        this.token = apiToken;
        this.refreshToken = refreshToken;
        this.apiType = apiType;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getApiToken() {
        return token;
    }

    public void setApiToken(String apiToken) {
        this.token = apiToken;
    }

    public String getApiType() {
        return apiType;
    }

    public void setApiType(String apiType) {
        this.apiType = apiType;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
