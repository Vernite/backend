package com.workflow.workflow.project;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.status.Status;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
@JsonInclude(Include.NON_NULL)
public class Project {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private Set<ProjectWorkspace> projectWorkspace = new TreeSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private Set<Status> statuses = new HashSet<>();

    @OneToOne(mappedBy = "project")
    private GitHubIntegration gitHubIntegration;

    public Project() {
    }

    public Project(String name) {
        this.name = name;
    }

    /**
     * This constructor creates new project from post request data.
     * 
     * @param request - post request data.
     * @throws ResponseStatusException - bad request when request.name is null.
     */
    public Project(ProjectRequest request) {
        this(request.getName());
        if (this.name == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * This method modifies project based on put request data.
     * 
     * @param request - put request data.
     * @throws ResponseStatusException - bad request when request.name is null.
     */
    public void put(ProjectRequest request) {
        if (request.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        this.name = request.getName();
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<ProjectWorkspace> getProjectWorkspace() {
        return projectWorkspace;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProjectWorkspace(Set<ProjectWorkspace> projectWorkspace) {
        this.projectWorkspace = projectWorkspace;
    }

    @JsonIgnore
    public List<ProjectMember> getProjectMembers() {
        return projectWorkspace.stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    public List<Status> getStatuses() {
        List<Status> statusList = new ArrayList<>(statuses);
        statusList.sort((first, second) -> first.getOrdinal() < second.getOrdinal() ? -1 : 1);
        return statusList;
    }

    public void setStatuses(Set<Status> statuses) {
        this.statuses = statuses;
    }

    public String getGitHubIntegration() {
        return gitHubIntegration != null ? gitHubIntegration.getRepositoryFullName() : null;
    }

    public void setGitHubIntegration(GitHubIntegration gitHubIntegration) {
        this.gitHubIntegration = gitHubIntegration;
    }
}
