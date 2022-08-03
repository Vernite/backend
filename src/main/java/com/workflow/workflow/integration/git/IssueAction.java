package com.workflow.workflow.integration.git;

/**
 * Enum representing a Git issue action.
 * Possible values are:
 * <ul>
 * <li>{@link #ATTACH}</li>
 * <li>{@link #DETACH}</li>
 * <li>{@link #CREATE}</li>
 * </ul>
 * {@link #ATTACH} value also contains {@link #issue} field.
 */
public enum IssueAction {
    ATTACH, DETACH, CREATE;

    private Issue issue;

    public Issue getIssue() {
        return issue;
    }

    void setIssue(Issue issue) {
        this.issue = issue;
    }
}
