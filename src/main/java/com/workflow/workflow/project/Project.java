package com.workflow.workflow.project;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.utils.SoftDeleteEntity;

/**
 * Entity for representing project.
 */
@Entity
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
@JsonInclude(Include.NON_NULL)
public class Project extends SoftDeleteEntity implements Comparable<Project> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    private List<ProjectWorkspace> projectWorkspaces = new ArrayList<>();

    @JsonIgnore
    @OneToMany(cascade = CascadeType.ALL, mappedBy = "project")
    @OrderBy("ordinal")
    private List<Status> statuses = new ArrayList<>();

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "project")
    private GitHubIntegration gitHubIntegration;

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    public Project(ProjectRequest request) {
        this(request.getName());
    }

    /**
     * Applies changes contained in request object to project.
     * 
     * @param request must not be {@literal null}. Can contain {@literal null} in
     *                fields. If field is {@literal null} it is assumed there is no
     *                changes for that field.
     */
    public void apply(ProjectRequest request) {
        if (request.getName() != null) {
            name = request.getName();
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<ProjectWorkspace> getProjectWorkspaces() {
        return projectWorkspaces;
    }

    public void setProjectWorkspaces(List<ProjectWorkspace> projectWorkspaces) {
        this.projectWorkspaces = projectWorkspaces;
    }

    public List<Status> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<Status> statuses) {
        this.statuses = statuses;
    }

    public String getGitHubIntegration() {
        return gitHubIntegration != null && gitHubIntegration.getActive() == null
                ? gitHubIntegration.getRepositoryFullName()
                : null;
    }

    public void setGitHubIntegration(GitHubIntegration gitHubIntegration) {
        this.gitHubIntegration = gitHubIntegration;
    }

    @JsonIgnore
    public List<ProjectMember> getProjectMembers() {
        return projectWorkspaces.stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(id);
        hash = prime * hash + (name == null ? 0 : name.hashCode());
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        Project other = (Project) obj;
        if (id != other.id)
            return false;
        if (name == null)
            return other.name == null;
        return name.equals(other.name);
    }

    @Override
    public int compareTo(Project other) {
        return name.equals(other.name) ? Long.compare(id, other.id) : name.compareTo(other.name);
    }
}
