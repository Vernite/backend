package com.workflow.workflow.workspace;

import java.util.Optional;

import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

public class WorkspaceRequest {
    @Schema(maxLength = 50, minLength = 1, description = "The name of the workspace. Trailing and leading whitespaces are removed.")
    private Optional<String> name = Optional.empty();

    public WorkspaceRequest() {
    }

    public WorkspaceRequest(String name) {
        this.name = Optional.ofNullable(name);
    }

    /**
     * Creates a new workspace entity from workspace request.
     * 
     * @param id   the id of the workspace.
     * @param user the user of the workspace.
     * @return the workspace entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Workspace createEntity(long id, User user) {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", "missing"));
        return new Workspace(id, user, nameString);
    }

    public Optional<String> getName() {
        return name;
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
}
