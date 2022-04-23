package com.workflow.workflow.projectworkspace;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.Workspace;

@Embeddable
public class ProjectWorkspaceKey implements Serializable {
    @Column(name = "workspace_id") Long workspaceId; 
    @Column(name = "project_id") Long projectId;
    
    public ProjectWorkspaceKey() {}

    public ProjectWorkspaceKey(Workspace workspace, Project project) {
        this.projectId = project.getId();
        this.workspaceId = workspace.getId();
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getWorkspaceId() {
        return workspaceId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }
    
    public void setWorkspaceId(Long workspaceId) {
        this.workspaceId = workspaceId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((projectId == null) ? 0 : projectId.hashCode());
        result = prime * result + ((workspaceId == null) ? 0 : workspaceId.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProjectWorkspaceKey other = (ProjectWorkspaceKey) obj;
        if (projectId == null) {
            if (other.projectId != null)
                return false;
        } else if (!projectId.equals(other.projectId))
            return false;
        if (workspaceId == null) {
            if (other.workspaceId != null)
                return false;
        } else if (!workspaceId.equals(other.workspaceId))
            return false;
        return true;
    }
}
