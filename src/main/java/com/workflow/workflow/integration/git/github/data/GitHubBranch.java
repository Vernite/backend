package com.workflow.workflow.integration.git.github.data;

public class GitHubBranch {
    private String ref;

    public GitHubBranch() {
    }

    public GitHubBranch(String ref) {
        this.ref = ref;
    }

    public String getRef() {
        return ref;
    }

    public void setRef(String ref) {
        this.ref = ref;
    }
}
