package com.workflow.workflow.integration.git.github.data;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.task.Task;

/**
 * Object to represent a GitHub Rest api issue.
 */
public class GitHubIssue {
    private long number;
    private String url;
    private String state;
    private String title;
    private String body;
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private List<String> assignees;

    public GitHubIssue() {
    }

    public GitHubIssue(long number, String url, String state, String title, String body) {
        this.number = number;
        this.url = url;
        this.state = state;
        this.title = title;
        this.body = body;
    }

    public GitHubIssue(Task task, List<String> assignees) {
        this.title = task.getName();
        this.body = task.getDescription();
        this.state = task.getStatus().isFinal() ? "closed" : "open";
        this.assignees = assignees;
    }

    public Issue toIssue() {
        return new Issue(number, url.replace("api.", ""), title, body, "github");
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
        return body == null ? "" : body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public List<String> getAssignees() {
        return assignees;
    }

    public void setAssignees(List<String> assignees) {
        this.assignees = assignees;
    }
}
