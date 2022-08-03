package com.workflow.workflow.integration.git.github.data;

/**
 * Object to represent a GitHub Rest api branch.
 */
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
