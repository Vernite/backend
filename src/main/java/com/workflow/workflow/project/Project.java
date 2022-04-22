package com.workflow.workflow.project;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Entity
public class Project {
    private @Id @GeneratedValue(strategy = GenerationType.IDENTITY) Long id;
    private String name;

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
     * This method modifies project based on patch request data.
     * @param request - patch request data.
     * @throws ResponseStatusException - bad request when request.name is null.
     */
    public void patch(ProjectRequest request) {
        if (request.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        this.name = request.getName();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
