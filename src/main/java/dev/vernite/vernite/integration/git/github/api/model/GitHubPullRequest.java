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

package dev.vernite.vernite.integration.git.github.api.model;

import java.util.List;

import dev.vernite.vernite.integration.git.PullRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Object to represent a GitHub Rest api pull request.
 */
@Data
@NoArgsConstructor
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
public class GitHubPullRequest extends GitHubIssue {

    private GitHubBranch head;

    private boolean merged;

    /**
     * Constructor for GitHubPullRequest.
     * 
     * @param number    number of the pull request
     * @param state     state of the pull request
     * @param title     title of the pull request
     * @param body      body of the pull request
     * @param assignees list of assignees
     * @param head      head of the pull request
     * @param merged    merged status of the pull request
     */
    public GitHubPullRequest(long number, String url, String state, String title, String body, List<String> assignees,
            GitHubBranch head, boolean merged) {
        super(number, url, state, title, body, assignees);
        this.head = head;
        this.merged = merged;
    }

    /**
     * Converts the GitHubPullRequest to a PullRequest.
     */
    public PullRequest toPullRequest() {
        return new PullRequest(getNumber(), getUrl().replace("api.", "").replace("/repos", ""), getTitle(), getBody(),
                "github", getHead().getRef());
    }

}
