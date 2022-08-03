package com.workflow.workflow.integration.git.github.data;

import java.util.List;

/**
 * Object to represent a GitHub Rest api repository list.
 */
public class GitHubInstallationRepositories {
    private List<GitHubRepository> repositories = List.of();

    public List<GitHubRepository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<GitHubRepository> repositories) {
        this.repositories = repositories;
    }
}
