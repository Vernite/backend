package com.workflow.workflow.integration.git.github.entity;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.task.Task;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubTask extends SoftDeleteEntity {
    @EmbeddedId
    private GitHubTaskKey id;

    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Task task;

    @MapsId("integrationId")
    @JoinColumn(name = "integration_id")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GitHubIntegration gitHubIntegration;

    private long issueId;

    private boolean isPullRequest;

    public GitHubTask() {
    }

    public GitHubTask(Task task, GitHubIntegration gitHubIntegration, long issueId, boolean isPullRequest) {
        this.id = new GitHubTaskKey(task, gitHubIntegration);
        this.task = task;
        this.gitHubIntegration = gitHubIntegration;
        this.issueId = issueId;
        this.isPullRequest = isPullRequest;
    }

    public GitHubTaskKey getId() {
        return id;
    }

    public void setId(GitHubTaskKey id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public GitHubIntegration getGitHubIntegration() {
        return gitHubIntegration;
    }

    public void setGitHubIntegration(GitHubIntegration gitIntegration) {
        this.gitHubIntegration = gitIntegration;
    }

    public long getIssueId() {
        return issueId;
    }

    public void setIssueId(long issueId) {
        this.issueId = issueId;
    }

    public boolean isPullRequest() {
        return isPullRequest;
    }

    public void setPullRequest(boolean isPullRequest) {
        this.isPullRequest = isPullRequest;
    }

    public String getLink() {
        return String.format("https://github.com/%s/%s/%d", getGitHubIntegration().getRepositoryFullName(), isPullRequest() ? "pull" : "issues", getIssueId());
    }
}
