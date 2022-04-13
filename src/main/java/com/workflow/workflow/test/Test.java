package com.workflow.workflow.test;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class Test {

    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;
    private String role;

    Test() {}

    Test(String name, String role) {
        this.name = name;
        this.role = role;
    }

    Test(TestRequest employeeRequest) {
        this.name = employeeRequest.getName();
        this.role = employeeRequest.getRole();
    }

    public void patch(TestRequest employeeRequest) {
        this.name = employeeRequest.getName();
        this.role = employeeRequest.getRole();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getRole() {
        return role;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setRole(String role) {
        this.role = role;
    }
}
