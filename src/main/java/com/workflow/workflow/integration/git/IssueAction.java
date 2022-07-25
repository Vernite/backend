package com.workflow.workflow.integration.git;

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
