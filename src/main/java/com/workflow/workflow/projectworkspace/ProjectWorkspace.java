package com.workflow.workflow.projectworkspace;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.entity.Workspace;

@Entity
public class ProjectWorkspace {
    @EmbeddedId
    private ProjectWorkspaceKey id;

    @ManyToOne
    @MapsId("workspaceId")
    private Workspace workspace;

    @ManyToOne
    @MapsId("projectId")
    @JoinColumn(name = "project_id")
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

    public Workspace getWorkspace() {
        return workspace;
    }

    public Project getProject() {
        return project;
    }

    public Long getPrivileges() {
        return privileges;
    }

    public void setId(ProjectWorkspaceKey id) {
        this.id = id;
    }

    public void setWorkspace(Workspace workspace) {
        this.workspace = workspace;
    }

    public void setProject(Project project) {
        this.project = project;
    }

    public void setPrivileges(Long privileges) {
        this.privileges = privileges;
    }

    public ProjectWithPrivileges getProjectWithPrivileges() {
        return new ProjectWithPrivileges(project, privileges);
    }

    public ProjectMember getProjectMember() {
        return new ProjectMember(workspace.getUser(), privileges);
    }
}
