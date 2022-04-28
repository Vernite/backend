package com.workflow.workflow.integration.git;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.MapsId;
import javax.persistence.OneToOne;

import com.workflow.workflow.task.Task;

@Entity
public class GitTask {
    @EmbeddedId
    private GitTaskKey id;

    @OneToOne
    @MapsId("taskId")
    @JoinColumn(name = "task_id")
    private Task task;

    @OneToOne
    @MapsId("integrationId")
    @JoinColumn(name = "integration_id")
    private GitIntegration gitIntegration;

    private long issueId;

    public GitTask() {}

    public GitTask(Task task, GitIntegration gitIntegration, long issueId) {
        this.id = new GitTaskKey(task, gitIntegration);
        this.task = task;
        this.gitIntegration = gitIntegration;
        this.issueId = issueId;
    }

    public GitTaskKey getId() {
        return id;
    }

    public void setId(GitTaskKey id) {
        this.id = id;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public GitIntegration getGitIntegration() {
        return gitIntegration;
    }
    public void setGitIntegration(GitIntegration gitIntegration) {
        this.gitIntegration = gitIntegration;
    }

    public long getIssueId() {
        return issueId;
    }
    
    public void setIssueId(long issueId) {
        this.issueId = issueId;
    }
}
