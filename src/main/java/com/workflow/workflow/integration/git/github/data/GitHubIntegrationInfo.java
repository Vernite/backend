package com.workflow.workflow.integration.git.github.data;

import java.util.List;

/**
 * Object to represent repositories an installation link.
 */
public class GitHubIntegrationInfo {
    private String link;
    private List<GitHubRepository> gitRepositories;

    public GitHubIntegrationInfo(String link, List<GitHubRepository> gitHubRepositories) {
        this.link = link;
        this.gitRepositories = gitHubRepositories;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public List<GitHubRepository> getGitRepositories() {
        return gitRepositories;
    }

    public void setGitRepositories(List<GitHubRepository> gitRepositories) {
        this.gitRepositories = gitRepositories;
    }
}
