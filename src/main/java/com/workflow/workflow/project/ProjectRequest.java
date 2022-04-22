package com.workflow.workflow.project;

public class ProjectRequest {
    private String name;

    public ProjectRequest() {}

    public ProjectRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
