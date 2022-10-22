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

package com.workflow.workflow.projectworkspace;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.Workspace;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Entity for representing relation between workspace (with user) and project.
 * Constrains privileges of user in project.
 */
@Entity
public class ProjectWorkspace {
    @EmbeddedId
    private ProjectWorkspaceKey id;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("workspaceId")
    private Workspace workspace;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("projectId")
    private Project project;

    Long privileges;

    public ProjectWorkspace() {
    }

    public ProjectWorkspace(Project project, Workspace workspace, Long privileges) {
        this.id = new ProjectWorkspaceKey(workspace, project);
        this.project = project;
        this.workspace = workspace;
        this.privileges = privileges;
    }

    public ProjectWorkspaceKey getId() {
        return id;
    }

    public void setId(ProjectWorkspaceKey id) {
        this.id = id;
    }

    public Workspace getWorkspace() {
        return workspace;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public Project getProject() {
        return project;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public Long getPrivileges() {
        return privileges;
    }

    public void setPrivileges(Long privileges) {
        this.privileges = privileges;
    }

    public ProjectWithPrivileges getProjectWithPrivileges() {
        return new ProjectWithPrivileges(getProject(), getPrivileges());
    }

    public ProjectMember getProjectMember() {
        return new ProjectMember(getWorkspace().getUser(), getPrivileges());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + (getId() == null ? 0 : getId().hashCode());
        hash = prime * hash + Long.hashCode(getPrivileges());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectWorkspace other = (ProjectWorkspace) obj;
        if (getPrivileges() != other.getPrivileges())
            return false;
        if (getId() == null)
            return other.getId() == null;
        return getId().equals(other.getId());
    }
}
