package com.workflow.workflow.workspace;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.user.User;

@Entity
public class Workspace {

    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    Workspace() {}

    Workspace(String name, User user) {
        this.name = name;
        this.user = user;
    }

    Workspace(WorkspaceRequest request, User user) {
        this(request.getName(), user);
    }

    public void patch(WorkspaceRequest request) {
        this.name = request.getName();
    }
    
    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
