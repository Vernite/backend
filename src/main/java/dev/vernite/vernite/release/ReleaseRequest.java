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

package dev.vernite.vernite.release;

import java.util.Date;
import java.util.Optional;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.utils.FieldErrorException;
import io.swagger.v3.oas.annotations.media.Schema;

public class ReleaseRequest {
    private static final String NULL_VALUE = "null value";

    @Schema(maxLength = 50, minLength = 1, description = "The name of the sprint. Trailing and leading whitespace are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(description = "The description of the sprint. Trailing and leading whitespace are removed.")
    private Optional<String> description = Optional.empty();
    @Schema(description = "Estimated time to publish release.")
    private Optional<Date> deadline = Optional.empty();
    @Schema(description = "Whether release has been released. Ignored when creating release.")
    private Optional<Boolean> released = Optional.empty();

    public ReleaseRequest() {
    }

    public ReleaseRequest(String name, String description, Date deadline, boolean released) {
        this.name = Optional.ofNullable(name);
        this.description = Optional.ofNullable(description);
        this.deadline = Optional.ofNullable(deadline);
        this.released = Optional.ofNullable(released);
    }

    /**
     * Creates a new release entity from sprint request.
     * 
     * @param project the project of the sprint.
     * @return the sprint entity.
     * @throws FieldErrorException if the request is invalid.
     */
    public Release createEntity(Project project) {
        String nameString = getName().orElseThrow(() -> new FieldErrorException("name", NULL_VALUE));
        Release release = new Release(nameString, project);
        getDescription().ifPresent(release::setDescription);
        getDeadline().ifPresent(release::setDeadline);
        return release;
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
        if (description != null) {
            description = description.trim();
        }
        this.description = Optional.ofNullable(description);
    }

    public Optional<Date> getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = Optional.ofNullable(deadline);
    }

    public Optional<Boolean> getReleased() {
        return released;
    }

    public void setReleased(Boolean released) {
        if (released == null) {
            throw new FieldErrorException("released", NULL_VALUE);
        }
        this.released = Optional.of(released);
    }
}
