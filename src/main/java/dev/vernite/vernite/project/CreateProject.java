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

package dev.vernite.vernite.project;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Class containing information needed to create new project entity.
 * Has required constraints annotated using Java Bean Validation.
 */
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class CreateProject {

    /**
     * Name for new project. Must contain at least one non-whitespace character.
     */
    @Setter
    @Getter
    @Size(min = 1, max = 50, message = "project name must be shorter than 50 characters")
    @NotBlank(message = "project name must contain at least one non-whitespace character")
    private String name;

    /**
     * Description for new project.
     */
    @Setter
    @Getter
    @NotNull(message = "description cannot be null")
    @Size(max = 1000, message = "project description must be shorter than 1000 characters")
    private String description;

    /**
     * Workspace ID for new project.
     */
    @Setter
    @Getter
    @NotNull(message = "workspace ID cannot be null")
    @Positive(message = "workspace ID must be positive")
    private Long workspaceId;

}
