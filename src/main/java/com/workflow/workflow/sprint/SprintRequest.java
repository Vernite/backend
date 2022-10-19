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

package com.workflow.workflow.sprint;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class SprintRequest {
    private static final String NULL_VALUE = "null value";
    private static final String START_FIELD = "startDate";
    private static final String FINISH_FIELD = "finishDate";

    @Schema(maxLength = 50, minLength = 1, description = "The name of the sprint. Trailing and leading whitespaces are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(description = "The description of the sprint. Trailing and leading whitespaces are removed.")
    private Optional<String> description = Optional.empty();
    @Schema(description = "The start date of the sprint. Must be before the finish date.")
    private Optional<Date> startDate = Optional.empty();
    @Schema(description = "The finish date of the sprint. Must be after the start date.")
    private Optional<Date> finishDate = Optional.empty();
    @Schema(description = "The status of the sprint. Must be one of the following: [\"planned\", \"in progress\", \"finished\", \"cancelled\"].")
    private Optional<String> status = Optional.empty();

    public SprintRequest() {
    }

    public SprintRequest(String name, String description, Date startDate, Date finishDate, String status) {
        this.name = Optional.ofNullable(name);
        this.description = Optional.ofNullable(description);
        this.startDate = Optional.ofNullable(startDate);
        this.finishDate = Optional.ofNullable(finishDate);
        this.status = Optional.ofNullable(status);
    }

    /**
     * Creates a new sprint entity from sprint request.
     * 
     * @param id      the id of the sprint.
     * @param project the project of the sprint.
     * @return the sprint entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Sprint createEntity(long id, Project project) {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", NULL_VALUE));
        String descString = getDescription().orElseThrow(() -> new FieldErrorException("description", NULL_VALUE));
        Date start = getStartDate().orElseThrow(() -> new FieldErrorException(START_FIELD, NULL_VALUE));
        Date finish = getFinishDate().orElseThrow(() -> new FieldErrorException(FINISH_FIELD, NULL_VALUE));
        String statusString = getStatus().orElseThrow(() -> new FieldErrorException("status", NULL_VALUE));
        return new Sprint(id, nameString, start, finish, statusString, descString, project);
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new FieldErrorException("name", NULL_VALUE);
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

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description == null) {
            throw new FieldErrorException("description", NULL_VALUE);
        }
        this.description = Optional.ofNullable(description.trim());
    }

    public Optional<Date> getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        if (startDate == null) {
            throw new FieldErrorException(START_FIELD, NULL_VALUE);
        }
        if (finishDate.isPresent() && startDate.after(finishDate.get())) {
            throw new FieldErrorException(START_FIELD, "after finishDate");
        }
        this.startDate = Optional.ofNullable(startDate);
    }

    public Optional<Date> getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        if (finishDate == null) {
            throw new FieldErrorException(FINISH_FIELD, NULL_VALUE);
        }
        if (startDate.isPresent() && finishDate.before(startDate.get())) {
            throw new FieldErrorException(FINISH_FIELD, "before startDate");
        }
        this.finishDate = Optional.ofNullable(finishDate);
    }

    public Optional<String> getStatus() {
        return status;
    }

    public void setStatus(String status) {
        if (status == null) {
            throw new FieldErrorException("status", NULL_VALUE);
        }
        this.status = Optional.of(status);
    }
}
