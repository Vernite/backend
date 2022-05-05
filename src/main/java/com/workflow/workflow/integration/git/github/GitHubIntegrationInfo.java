package com.workflow.workflow.integration.git.github;

import java.util.List;

import com.workflow.workflow.integration.git.github.service.GitHubRepository;

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
