package com.workflow.workflow.workspace;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.projectworkspace.ProjectWithPrivileges;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
public class Workspace {

    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;
    @ManyToOne
    @JoinColumn(name = "user_id")
    @JsonIgnore
    private User user;

    @OneToMany(mappedBy = "workspace")
    private Set<ProjectWorkspace> projectWorkspace;

    public Workspace() {}

    public Workspace(String name, User user) {
        this.name = name;
        this.user = user;
    }

    /**
     * This constructor creates new workspace from post request data for given user.
     * @param request - post request data.
     * @param user - owner of new workspace.
     * @throws ResponseStatusException - bad request when either user or request.name is null.
     */
    public Workspace(WorkspaceRequest request, User user) {
        this(request.getName(), user);
        if (this.name == null || this.user == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * This method modifies workspace based on patch request data.
     * @param request - patch request data
     * @throws ResponseStatusException - bad request when either user or request.name is null.
     */
    public void patch(WorkspaceRequest request) {
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

    public User getUser() {
        return user;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public List<ProjectWithPrivileges> getProjectsWithPrivileges() {
        return projectWorkspace.stream().map(ProjectWorkspace::getProjectWithPrivileges).toList();
    }
}
