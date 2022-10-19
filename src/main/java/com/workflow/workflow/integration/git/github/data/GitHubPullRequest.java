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

import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.task.Task;

/**
 * Object to represent a GitHub Rest api pull request.
 */
public class GitHubPullRequest extends GitHubIssue {
    private GitHubBranch head;
    private boolean merged = false;

    public GitHubPullRequest() {
    }

    public GitHubPullRequest(long number, String url, String state, String title, String body, GitHubBranch head) {
        super(number, url, state, title, body);
        this.head = head;
    }

    public GitHubPullRequest(Task task, List<String> assignees) {
        super(task, assignees);
    }

    public PullRequest toPullRequest() {
        return new PullRequest(getNumber(),
                getUrl() != null ? getUrl().replace("api.", "").replace("/repos", "") : null, getTitle(),
                getBody(), "github", head.getRef());
    }

    public GitHubBranch getHead() {
        return head;
    }

    public void setHead(GitHubBranch head) {
        this.head = head;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }
}
