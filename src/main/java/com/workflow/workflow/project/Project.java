package com.workflow.workflow.project;

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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.status.Status;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
public class Project {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "project")
    private Set<ProjectWorkspace> projectWorkspace = new TreeSet<>();

    @JsonIgnore
    @OneToMany(mappedBy = "project", fetch = FetchType.LAZY)
    private Set<Status> statuses = new HashSet<>();

    public Project() {}

    public Project(String name) {
        this.name = name;
    }

    /**
     * This constructor creates new project from post request data.
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

    public List<ProjectMember> getProjectMembers() {
        return projectWorkspace.stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    public Set<Status> getStatuses() {
        return statuses;
    }

    public void setStatuses(Set<Status> statuses) {
        this.statuses = statuses;
    }
}
