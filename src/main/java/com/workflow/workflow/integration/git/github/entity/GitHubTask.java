package com.workflow.workflow.integration.git.github.entity;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubTask extends SoftDeleteEntity {
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

    @Column(length = 50)
    private String title;

    @Lob
    private String description;

    @Column(length = 50)
    private String branch;

    private byte isPullRequest;

    public GitHubTask() {
    }

    public GitHubTask(Task task, GitHubIntegration gitHubIntegration, GitHubIssue issue, byte isPullRequest) {
        this.id = new GitHubTaskKey(task, gitHubIntegration);
        this.task = task;
        this.gitHubIntegration = gitHubIntegration;
        this.issueId = issue.getNumber();
        this.isPullRequest = isPullRequest;
        this.title = issue.getTitle();
        this.description = issue.getBody();
        if (issue instanceof GitHubPullRequest) {
            this.branch = ((GitHubPullRequest) issue).getHead().getRef();
        }
    }

    @Deprecated
    public GitHubTask(Task task, GitHubIntegration gitHubIntegration, long issueId, byte isPullRequest) {
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

    public byte getIsPullRequest() {
        return isPullRequest;
    }

    public void setIsPullRequest(byte isPullRequest) {
        this.isPullRequest = isPullRequest;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getState() {
        if (getIsPullRequest() == 2) {
            return "merged";
        }
        if (getTask().getStatus().isFinal()) {
            return "closed";
        }
        return "open";
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getLink() {
        return String.format("https://github.com/%s/%s/%d", getGitHubIntegration().getRepositoryFullName(),
                getIsPullRequest() != 0 ? "pull" : "issues", getIssueId());
    }

    public Issue toIssue() {
        if (getIsPullRequest() != 0) {
            return new PullRequest(getIssueId(), getLink(), getTitle(), getDescription(), "github", getBranch());
        } else {
            return new Issue(getIssueId(), getLink(), getTitle(), getDescription(), "github");
        }
    }
}
