package com.workflow.workflow.project;

public class ProjectRequest {
    private String name;
    private Long workspaceId;

    public ProjectRequest() {
    }

    public ProjectRequest(String name, Long workspaceId) {
        this.name = name;
        this.workspaceId = workspaceId;
    }

    public String getName() {
        return name;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
