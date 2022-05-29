package com.workflow.workflow.integration.git.github.data;

import com.fasterxml.jackson.annotation.JsonProperty;

public class GitHubRepository {
    private long id;
    private String fullName;
    private boolean isPrivate;

    public GitHubRepository() {
    }

    public GitHubRepository(long id, String fullName, boolean isPrivate) {
        this.id = id;
        this.fullName = fullName;
        this.isPrivate = isPrivate;
    }

    @JsonProperty("full_name")
    public String getFullName() {
        return fullName;
    }

    @JsonProperty("fullName")
    public String getFullName2() {
        return fullName;
    }

    @JsonProperty("full_name")
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    @JsonProperty("private")
    public boolean getIsPrivate() {
        return isPrivate;
    }

    @JsonProperty("private")
    public void setPrivate(boolean isPrivate) {
        this.isPrivate = isPrivate;
    }
}
