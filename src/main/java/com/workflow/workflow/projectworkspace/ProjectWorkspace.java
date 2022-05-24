package com.workflow.workflow.projectworkspace;

import javax.persistence.CascadeType;
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
 * Constains privillages of user in project.
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
        if (this == obj)
            return true;
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
