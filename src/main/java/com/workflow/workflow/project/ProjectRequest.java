package com.workflow.workflow.project;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class ProjectRequest {
    @Schema(maxLength = 50, minLength = 1, description = "The name of the project. Trailing and leading whitespaces are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(description = "When creating project will be created in this workspace. When updating project will be moved to this workspace.")
    private Optional<Long> workspaceId = Optional.empty();

    public ProjectRequest() {
    }

    public ProjectRequest(String name, Long workspaceId) {
        this.name = Optional.ofNullable(name);
        this.workspaceId = Optional.ofNullable(workspaceId);
    }

    /**
     * Creates a new project entity from project request.
     * 
     * @return the project entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Project createEntity() {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", "missing"));
        return new Project(nameString);
    }

    public Optional<String> getName() {
        return name;
    }

    public Optional<Long> getWorkspaceId() {
        return workspaceId;
    }

    public void setName(String name) {
        if (name == null) {
            throw new FieldErrorException("name", "null value");
        }
        name = name.trim();
        if (name.isEmpty()) {
            throw new FieldErrorException("name", "empty");
        }
        if (name.length() > 50) {
            throw new FieldErrorException("name", "too long");
        }
        this.name = Optional.of(name);
    }

    public void setWorkspaceId(Long workspaceId) {
        if (workspaceId == null) {
            throw new FieldErrorException("workspaceId", "null value");
        }
        this.workspaceId = Optional.of(workspaceId);
    }
}
