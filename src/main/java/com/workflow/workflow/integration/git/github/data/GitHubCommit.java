package com.workflow.workflow.integration.git.github.data;

/**
 * Object to represent a GitHub Rest api commit.
 */
public class GitHubCommit {
    private String id;
    private String message;

    public GitHubCommit() {
    }

    public GitHubCommit(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
