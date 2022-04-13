package com.workflow.workflow.test;

public class TestRequest {
    private String name;
    private String role;

    TestRequest() {}

    TestRequest(String name, String role) {
        this.name = name;
        this.role = role;
    }

    public String getName() {
        return name;
    }
    
    public String getRole() {
        return role;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
