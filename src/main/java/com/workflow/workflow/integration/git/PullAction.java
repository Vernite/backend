package com.workflow.workflow.integration.git;

public enum PullAction {
    ATTACH, DETACH;

    private PullRequest pullRequest;

    public PullRequest getPullRequest() {
        return pullRequest;
    }

    void setPullRequest(PullRequest pullRequest) {
        this.pullRequest = pullRequest;
    }
}
