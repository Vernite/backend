package com.workflow.workflow.integration.git.github.data;

/**
 * Object to represent a GitHub Rest api installation.
 */
public class GitHubInstallationApi {
    private long id;
    private GitHubUser account;

    public GitHubInstallationApi() {
    }

    public GitHubInstallationApi(long id, GitHubUser account) {
        this.id = id;
        this.account = account;
    }

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
