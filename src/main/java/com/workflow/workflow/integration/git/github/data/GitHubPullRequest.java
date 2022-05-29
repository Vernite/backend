package com.workflow.workflow.integration.git.github.data;

import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.task.Task;

public class GitHubPullRequest extends GitHubIssue {
    private GitHubBranch head;

    public GitHubPullRequest() {
    }

    public GitHubPullRequest(Task task) {
        super(task);
    }

    public PullRequest toPullRequest() {
        return new PullRequest(getNumber(), getUrl().replace("api.", ""), getTitle(), getBody(), "github", head.getRef());
    }

    public GitHubBranch getHead() {
        return head;
    }

    public void setHead(GitHubBranch head) {
        this.head = head;
    }
}
