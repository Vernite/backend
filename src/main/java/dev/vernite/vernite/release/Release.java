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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.task.Task;

/**
 * Entity representing a release of a project.
 */
@Data
@NoArgsConstructor
@Entity(name = "releases")
public class Release {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = "release id must be non negative number")
    private long id;

    @Column(nullable = false, length = 50)
    @Size(min = 1, max = 50, message = "release name must be shorter than 50 characters")
    @NotBlank(message = "release name must contain at least one non-whitespace character")
    private String name;

    @Column(nullable = false, length = 1000)
    @NotNull(message = "release description must not be null")
    @Size(max = 1000, message = "release description must be shorter than 1000 characters")
    private String description;

    @NotNull(message = "release deadline must not be null")
    private Date deadline;

    private boolean released = false;

    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "release must belong to a project")
    private Project project;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "release")
    @NotNull(message = "release must have tasks")
    private List<Task> tasks = new ArrayList<>();

    @JsonIgnore
    private long gitReleaseId = 0;

    /**
     * Creates release with given name, description, deadline and project.
     * 
     * @param name        must not be {@literal null} and have at least one
     *                    non-whitespace
     * @param description must not be {@literal null} and have less than 1000
     *                    characters
     * @param deadline    must not be {@literal null}
     * @param project     must not be {@literal null} and must exist in database
     */
    public Release(String name, String description, Date deadline, Project project) {
        setName(name);
        setDescription(description);
        this.deadline = deadline;
        this.project = project;
    }

    /**
     * Constructs release from create release request.
     * 
     * @param project must not be {@literal null} and must exist in database
     * @param create  must not be {@literal null} and must have valid fields
     */
    public Release(Project project, CreateRelease create) {
        this(create.getName(), create.getDescription(), create.getDeadline(), project);
    }

    /**
     * Updates release with non-empty update fields.
     * 
     * @param update must not be {@literal null}. When fields are not present in
     *               update, they are not updated.
     */
    public void update(UpdateRelease update) {
        if (update.getName() != null) {
            this.setName(update.getName());
        }
        if (update.getDescription() != null) {
            this.setDescription(update.getDescription());
        }
        if (update.getDeadline() != null) {
            this.setDeadline(update.getDeadline());
        }
    }

    /**
     * Setter for name value. It performs {@link String#trim()} on its argument.
     * 
     * @param name must not be {@literal null} and have at least one non-whitespace
     *             character and less than 50 characters
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Setter for description value. It performs {@link String#trim()} on its
     * argument.
     * 
     * @param description must not be {@literal null} and have at least one
     *                    non-whitespace character and less than 1000 characters
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

}
