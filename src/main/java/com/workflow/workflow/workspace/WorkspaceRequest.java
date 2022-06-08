package com.workflow.workflow.workspace;

import io.swagger.v3.oas.annotations.media.Schema;

public class WorkspaceRequest {
    @Schema(description = "The name of the project. Trailing and leading whitespaces are removed. Cant be empty or longer than 50 characters.")
    private String name;

    public WorkspaceRequest() {
    }

    public WorkspaceRequest(String name) {
        setName(name);
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name == null ? null : name.trim();
    }
}
