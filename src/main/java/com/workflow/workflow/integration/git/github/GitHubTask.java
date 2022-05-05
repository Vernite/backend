package com.workflow.workflow.integration.git.github;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import com.workflow.workflow.task.Task;

@Entity
public class GitHubTask {
    @EmbeddedId
    private GitHubTaskKey id;

    @OneToOne
    @MapsId("taskId")
    @JoinColumn(name = "task_id")
    private Task task;

    @OneToOne
    @MapsId("integrationId")
    @JoinColumn(name = "integration_id")
    private GitHubIntegration gitHubIntegration;

    private long issueId;

    public GitHubTask() {}

    public GitHubTask(Task task, GitHubIntegration gitHubIntegration, long issueId) {
        this.id = new GitHubTaskKey(task, gitHubIntegration);
        this.task = task;
        this.gitHubIntegration = gitHubIntegration;
        this.issueId = issueId;
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
}
