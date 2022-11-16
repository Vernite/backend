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

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.task.Task;

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
