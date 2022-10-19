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
