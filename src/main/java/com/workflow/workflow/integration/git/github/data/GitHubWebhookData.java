package com.workflow.workflow.integration.git.github.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Object to represent GitHub Rest api webhook data.
 */
public class GitHubWebhookData {
    private String action;
    private GitHubRepository repository;
    private GitHubInstallationApi installation;
    @JsonProperty("repositories_removed")
    private List<GitHubRepository> repositoriesRemoved;
    private GitHubIssue issue;
    private List<GitHubCommit> commits;
    private String after;
    @JsonProperty("pull_request")
    private GitHubPullRequest pullRequest;
    private GitHubUser assignee;

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public GitHubIssue getIssue() {
        return issue;
    }

    public void setIssue(GitHubIssue issue) {
        this.issue = issue;
    }

    public List<GitHubRepository> getRepositoriesRemoved() {
        return repositoriesRemoved;
    }

    public void setRepositoriesRemoved(List<GitHubRepository> repositoriesRemoved) {
        this.repositoriesRemoved = repositoriesRemoved;
    }

    public GitHubRepository getRepository() {
        return repository;
    }

    public void setRepository(GitHubRepository repository) {
        this.repository = repository;
    }

    public GitHubInstallationApi getInstallation() {
        return installation;
    }

    public void setInstallation(GitHubInstallationApi installation) {
        this.installation = installation;
    }

    public String getAfter() {
        return after;
    }

    public void setAfter(String after) {
        this.after = after;
    }

    public List<GitHubCommit> getCommits() {
        return commits;
    }

    public void setCommits(List<GitHubCommit> commits) {
        this.commits = commits;
    }

    public GitHubPullRequest getPullRequest() {
        return pullRequest;
    }

    public void setPullRequest(GitHubPullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }

    public GitHubUser getAssignee() {
        return assignee;
    }

    public void setAssignee(GitHubUser assignee) {
        this.assignee = assignee;
    }
}
