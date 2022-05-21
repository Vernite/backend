package com.workflow.workflow.workspace.entity;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.workflow.workflow.projectworkspace.ProjectWithPrivileges;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.SoftDeleteEntity;
import com.workflow.workflow.workspace.WorkspaceRequest;

/**
 * Entity for representing workspace. Its primary key is composed of id and user
 * id. Workspace name cant be longer than 50 characters.
 */
@Entity
public class Workspace extends SoftDeleteEntity implements Comparable<Workspace> {
    @JsonUnwrapped
    @EmbeddedId
    private WorkspaceKey id;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("userId")
    private User user;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "workspace")
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    public Workspace() {
    }

    public Workspace(long id, User user, String name) {
        this.id = new WorkspaceKey(id);
        this.user = user;
        this.name = name;
    }

    public Workspace(long id, User user, WorkspaceRequest request) {
        this(id, user, request.getName());
    }

    public void apply(WorkspaceRequest request) {
        if (request.getName() != null) {
            name = request.getName();
        }
    }

    public WorkspaceKey getId() {
        return id;
    }

    public void setId(WorkspaceKey id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ProjectWorkspace> getProjectWorkspaces() {
        return projectWorkspaces;
    }

    public void setProjectWorkspaces(List<ProjectWorkspace> projectWorkspaces) {
        this.projectWorkspaces = projectWorkspaces;
    }

    public List<ProjectWithPrivileges> getProjectsWithPrivileges() {
        return projectWorkspaces.stream().filter(pw -> pw.getProject().getActive() == null)
                .sorted((first, second) -> {
                    int result = first.getProject().getName().compareTo(second.getProject().getName());
                    if (result != 0) {
                        return result;
                    }
                    return first.getProject().getId().compareTo(second.getProject().getId());
                })
                .map(ProjectWorkspace::getProjectWithPrivileges).toList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + id.hashCode();
        hash = prime * hash + name.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Workspace other = (Workspace) obj;
        return id.equals(other.id) && name.equals(other.name);
    }

    @Override
    public int compareTo(Workspace other) {
        return name.equals(other.name) ? id.compareTo(other.id) : name.compareTo(other.name);
    }
}
