/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

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
