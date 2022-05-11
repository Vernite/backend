package com.workflow.workflow.integration.git.github.service;

import java.util.List;

public class GitHubRepositoryList {
    private List<GitHubRepository> repositories;

    public List<GitHubRepository> getRepositories() {
        return repositories;
    }

    public void setRepositories(List<GitHubRepository> repositories) {
        this.repositories = repositories;
    }
}
