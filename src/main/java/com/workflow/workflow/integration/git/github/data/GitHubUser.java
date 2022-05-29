package com.workflow.workflow.integration.git.github.data;

public class GitHubUser {
    private long id;
    private String login;

    public GitHubUser() {
    }

    public GitHubUser(long id, String login) {
        this.id = id;
        this.login = login;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }
}
