package com.workflow.workflow.integration.git.github.data;

public class GitHubMergeInfo {
    private String sha;
    private boolean merged;
    private String message;

    public GitHubMergeInfo() {
    }

    public GitHubMergeInfo(String sha, boolean merged, String message) {
        this.sha = sha;
        this.merged = merged;
        this.message = message;
    }

    public String getSha() {
        return sha;
    }

    public void setSha(String sha) {
        this.sha = sha;
    }

    public boolean isMerged() {
        return merged;
    }

    public void setMerged(boolean merged) {
        this.merged = merged;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
