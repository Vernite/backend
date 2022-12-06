/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.integration.git.github.entity.task;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;

import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.task.Task;

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
