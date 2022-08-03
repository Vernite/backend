package com.workflow.workflow.integration.git.github.data;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object to represent GitHub Rest api repository.
 */
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
        return getFullName();
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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (id ^ (id >>> 32));
        result = prime * result + (isPrivate ? 1231 : 1237);
        result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final GitHubRepository other = (GitHubRepository) obj;
        if (this.id != other.id) {
            return false;
        }
        if ((this.fullName == null) ? (other.fullName != null) : !this.fullName.equals(other.fullName)) {
            return false;
        }
        return this.isPrivate == other.isPrivate;
    }
}
