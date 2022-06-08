package com.workflow.workflow.project;

import io.swagger.v3.oas.annotations.media.Schema;

public class ProjectRequest {
    @Schema(description = "The name of the project. Trailing and leading whitespaces are removed. Cant be empty or longer than 50 characters.")
    private String name;
    private Long workspaceId;

    public ProjectRequest() {
    }

    public ProjectRequest(String name, Long workspaceId) {
        setName(name);
        this.workspaceId = workspaceId;
    }

    public String getName() {
        return name;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }

    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }
}
