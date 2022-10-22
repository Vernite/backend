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

package com.workflow.workflow.status;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class StatusRequest {
    private static final String NULL_VALUE = "null value";

    @Schema(maxLength = 50, minLength = 1, description = "The name of the status. Trailing and leading whitespace are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(description = "The color of the status.")
    private Optional<Integer> color = Optional.empty();
    @Schema(description = "Whether the status is considered final. When column is final finished tasks are moved to this status.")
    private Optional<Boolean> isFinal = Optional.empty();
    @Schema(description = "Whether the status is considered begin. When column is begin started tasks are moved to this status.")
    private Optional<Boolean> isBegin = Optional.empty();
    @Schema(description = "The ordinal of the status.")
    private Optional<Integer> ordinal = Optional.empty();

    public StatusRequest() {
    }

    public StatusRequest(String name, Integer color, Boolean isFinal, Boolean isBegin, Integer ordinal) {
        this.name = Optional.ofNullable(name);
        this.color = Optional.ofNullable(color);
        this.isFinal = Optional.ofNullable(isFinal);
        this.isBegin = Optional.ofNullable(isBegin);
        this.ordinal = Optional.ofNullable(ordinal);
    }

    /**
     * Creates a new status entity from status request.
     * 
     * @param id      the id of the status.
     * @param project the project of the status.
     * @return the status entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Status createEntity(long id, Project project) {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", NULL_VALUE));
        Integer colorInt = getColor().orElseThrow(() -> new FieldErrorException("color", NULL_VALUE));
        Boolean isFinalBoolean = getFinal().orElseThrow(() -> new FieldErrorException("isFinal", NULL_VALUE));
        Boolean isBeginBoolean = getBegin().orElseThrow(() -> new FieldErrorException("isBegin", NULL_VALUE));
        Integer ordinalInt = getOrdinal().orElseThrow(() -> new FieldErrorException("ordinal", NULL_VALUE));
        return new Status(id, nameString, colorInt, isFinalBoolean, isBeginBoolean, ordinalInt, project);
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

    public Optional<Integer> getColor() {
        return color;
    }

    public void setColor(Integer color) {
        if (color == null) {
            throw new FieldErrorException("color", NULL_VALUE);
        }
        this.color = Optional.of(color);
    }

    public Optional<Boolean> getFinal() {
        return isFinal;
    }

    public void setFinal(Boolean isFinal) {
        if (isFinal == null) {
            throw new FieldErrorException("isFinal", NULL_VALUE);
        }
        this.isFinal = Optional.of(isFinal);
    }

    public Optional<Boolean> getBegin() {
        return isBegin;
    }

    public void setBegin(Boolean isBegin) {
        if (isBegin == null) {
            throw new FieldErrorException("isBegin", NULL_VALUE);
        }
        this.isBegin = Optional.of(isBegin);
    }

    public Optional<Integer> getOrdinal() {
        return ordinal;
    }

    public void setOrdinal(Integer ordinal) {
        if (ordinal == null) {
            throw new FieldErrorException("ordinal", NULL_VALUE);
        }
        this.ordinal = Optional.of(ordinal);
    }

}
