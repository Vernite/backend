/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.workspace;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.projectworkspace.ProjectWithPrivileges;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Entity for representing workspace. Workspace is a collection of projects. Its
 * primary key is composed of id and user id. Workspace with id 0 is reserved
 * for inbox.
 */
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Workspace extends SoftDeleteEntity implements Comparable<Workspace> {
    @EmbeddedId
    @JsonUnwrapped
    private WorkspaceKey id;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonIgnore
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "project_workspace",
        joinColumns = {
            @JoinColumn(name = "workspace_user_id", referencedColumnName = "user_id"),
            @JoinColumn(name = "workspace_id", referencedColumnName = "id")
        },
        inverseJoinColumns = @JoinColumn(name = "project_id", referencedColumnName = "id")
    )
    private Set<Project> projects = Set.of();

    @JsonIgnore
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "workspace")
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    public Workspace() {
    }

    public Workspace(long id, User user, String name) {
        this.id = new WorkspaceKey(id, user);
        this.user = user;
        this.name = name;
    }

    /**
     * Updates workspace with non-empty request fields.
     * 
     * @param request must not be {@literal null}. When fields are not present in
     *                request, they are not updated.
     */
    public void update(@NotNull WorkspaceRequest request) {
        request.getName().ifPresent(this::setName);
    }

    /**
     * Checks if the workspace is empty.
     * 
     * @return {@literal true} if the workspace is empty, {@literal false}
     *         otherwise.
     */
    public boolean isEmpty() {
        return getProjectWorkspaces().stream().allMatch(p -> p.getProject().getActive() != null);
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

    @Deprecated
    public List<ProjectWithPrivileges> getProjectsWithPrivileges() {
        return getProjectWorkspaces().stream()
                .filter(pw -> pw.getProject().getActive() == null)
                .map(ProjectWorkspace::getProjectWithPrivileges)
                .sorted((f, s) -> f.compareTo(s)).toList();
    }

    public Set<Project> getProjects() {
        return projects;
    }

    public void setProjects(Set<Project> projects) {
        this.projects = projects;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + (getId() == null ? 0 : getId().hashCode());
        hash = prime * hash + (getName() == null ? 0 : getName().hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        Workspace other = (Workspace) obj;
        if (getId() == null) {
            if (other.getId() != null)
                return false;
        } else if (!getId().equals(other.getId()))
            return false;
        if (getName() == null)
            return other.getName() == null;
        return getName().equals(other.getName());
    }

    @Override
    public int compareTo(Workspace o) {
        return getName().equals(o.getName()) ? getId().compareTo(o.getId()) : getName().compareTo(o.getName());
    }
}
