package com.workflow.workflow.projectworkspace;

import java.io.Serializable;

import javax.persistence.Embeddable;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceKey;

/**
 * Composite key for pivot table. Composed of workspace id and project id.
 */
@Embeddable
public class ProjectWorkspaceKey implements Serializable, Comparable<ProjectWorkspaceKey> {
    WorkspaceKey workspaceId;
    long projectId;

    public ProjectWorkspaceKey() {
    }

    public ProjectWorkspaceKey(Workspace workspace, Project project) {
        this.workspaceId = workspace.getId();
        this.projectId = project.getId();
    }

    public WorkspaceKey getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(WorkspaceKey workspaceId) {
        this.workspaceId = workspaceId;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(projectId);
        hash = prime * hash + (workspaceId == null ? 0 : workspaceId.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ProjectWorkspaceKey other = (ProjectWorkspaceKey) obj;
        if (projectId != other.projectId)
            return false;
        if (workspaceId == null)
            return other.workspaceId == null;
        return workspaceId.equals(other.workspaceId);
    }

    @Override
    public int compareTo(ProjectWorkspaceKey o) {
        return projectId == o.projectId ? workspaceId.compareTo(o.workspaceId) : Long.compare(projectId, o.projectId);
    }
}
