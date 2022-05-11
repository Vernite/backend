package com.workflow.workflow.integration.git.github.service;

import com.workflow.workflow.task.Task;

public class GitHubIssue {
    private String url;
    private String state;
    private String title;
    private String body;
    private long number;

    public GitHubIssue() {
    }

    public GitHubIssue(Task task) {
        this.title = task.getName();
        this.body = task.getDescription();
        if (Boolean.TRUE.equals(task.getStatus().isFinal())) {
            this.state = "closed";
        } else {
            this.state = "open";
        }
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }
}
