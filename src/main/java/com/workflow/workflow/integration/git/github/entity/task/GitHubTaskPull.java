package com.workflow.workflow.integration.git.github.entity.task;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.task.Task;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
public class GitHubTaskPull {
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

    private boolean merged;

    public GitHubTaskPull() {
    }

    public GitHubTaskPull(Task task, GitHubIntegration gitHubIntegration, GitHubPullRequest pull) {
        setId(new GitHubTaskKey(task, gitHubIntegration));
        setTask(task);
        setGitHubIntegration(gitHubIntegration);
        setIssueId(pull.getNumber());
        setMerged(false);
        setTitle(pull.getTitle());
        setDescription(pull.getBody());
        setBranch(pull.getHead().getRef());
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

    public boolean getMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
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
        if (getMerged()) {
            return "merged";
        }
        return getTask().getStatus().isFinal() ? "closed" : "open";
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getLink() {
        return String.format("https://github.com/%s/%s/%d", getGitHubIntegration().getRepositoryFullName(), "pull",
                getIssueId());
    }

    public PullRequest toPull() {
        PullRequest pull = new PullRequest(getIssueId(), getLink(), getTitle(), getDescription(), "github", getBranch());
        pull.setState(getState());
        return pull;
    }
}
