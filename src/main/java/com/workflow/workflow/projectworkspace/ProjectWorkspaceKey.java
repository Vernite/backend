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
}
