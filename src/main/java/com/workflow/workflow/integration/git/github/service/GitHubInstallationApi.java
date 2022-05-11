package com.workflow.workflow.integration.git.github.service;

public class GitHubInstallationApi {
    private long id;
    private GitHubUser account;

    public GitHubUser getAccount() {
        return account;
    }

    public void setAccount(GitHubUser account) {
        this.account = account;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
