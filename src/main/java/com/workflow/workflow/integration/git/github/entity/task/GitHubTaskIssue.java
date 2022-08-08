package com.workflow.workflow.integration.git.github.entity.task;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.task.Task;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubTaskIssue {
    @EmbeddedId
    private GitHubTaskKey id;

    @MapsId("taskId")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Task task;

    @MapsId("integrationId")
    @JoinColumn(name = "integration_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private GitHubIntegration gitHubIntegration;

    private long issueId;

    public GitHubTaskIssue() {
    }

    public GitHubTaskIssue(Task task, GitHubIntegration gitHubIntegration, GitHubIssue issue) {
        this.id = new GitHubTaskKey(task, gitHubIntegration);
        this.task = task;
        this.gitHubIntegration = gitHubIntegration;
        this.issueId = issue.getNumber();
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

    public String getState() {
        return getTask().getStatus().isFinal() ? "closed" : "open";
    }

    public String getLink() {
        return String.format("https://github.com/%s/%s/%d", getGitHubIntegration().getRepositoryFullName(), "issues",
                getIssueId());
    }

    public Issue toIssue() {
        Issue issue = new Issue(getIssueId(), getLink(), getTask().getName(), getTask().getDescription(), "github");
        issue.setState(getState());
        return issue;
    }
}
