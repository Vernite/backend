package com.workflow.workflow.integration.git;

/**
 * Enum representing a Git pull action.
 * Possible values are:
 * <ul>
 * <li>{@link #ATTACH}</li>
 * <li>{@link #DETACH}</li>
 * </ul>
 * {@link #ATTACH} value also contains {@link #pullRequest} field.
 */
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
