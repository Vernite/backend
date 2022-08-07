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
