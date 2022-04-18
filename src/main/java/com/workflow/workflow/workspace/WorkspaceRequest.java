package com.workflow.workflow.workspace;

public class WorkspaceRequest {

    private String name;

    WorkspaceRequest() {}

    WorkspaceRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
