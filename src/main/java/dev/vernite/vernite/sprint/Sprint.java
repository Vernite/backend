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

package dev.vernite.vernite.sprint;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.utils.FieldErrorException;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

/**
 * Entity representing a scrum sprint.
 */
@Data
@Entity
@NoArgsConstructor
public class Sprint {

    public enum Status {
        CREATED, ACTIVE, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = "Id must be positive or zero")
    private long id;

    @Column(nullable = false, length = 50)
    @NotBlank(message = "Name must not be blank")
    @Size(min = 1, max = 50, message = "status name must be shorter than 50 characters")
    private String name;

    @Column(nullable = false)
    @NotNull(message = "Start date must not be null")
    private Date startDate;

    @Column(nullable = false)
    @NotNull(message = "Finish date must not be null")
    private Date finishDate;

    @PositiveOrZero(message = "Status must be positive or zero")
    private int status;

    @Column(nullable = false, length = 1000)
    @NotNull(message = "description cannot be null")
    @Size(max = 1000, message = "description must be shorter than 1000 characters")
    private String description;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "Project must not be null")
    private Project project;

    @ToString.Exclude
    @OrderBy("name ASC")
    @EqualsAndHashCode.Exclude
    @NotNull(message = "Tasks must not be null")
    @OneToMany(mappedBy = "sprint", cascade = CascadeType.PERSIST)
    private List<Task> tasks = new ArrayList<>();

    @ToString.Exclude
    @OrderBy("name ASC")
    @EqualsAndHashCode.Exclude
    @NotNull(message = "Archive tasks must not be null")
    @ManyToMany(mappedBy = "archiveSprints", cascade = CascadeType.PERSIST)
    private List<Task> archiveTasks = new ArrayList<>();

    /**
     * Default constructor for Sprint.
     * 
     * @param name        must not be {@literal null} or empty
     * @param start       must not be {@literal null}
     * @param finish      must not be {@literal null}
     * @param status      must not be {@literal null}
     * @param description must not be {@literal null}
     * @param project     must not be {@literal null}
     */
    public Sprint(String name, Date start, Date finish, Status status, String description, Project project) {
        setName(name);
        this.startDate = start;
        this.finishDate = finish;
        this.status = status.ordinal();
        setDescription(description);
        this.project = project;

        if (getStartDate().after(getFinishDate())) {
            throw new FieldErrorException("date", "Start date must be before end date");
        }
    }

    /**
     * Constructor for Sprint from create request.
     * 
     * @param project must not be {@literal null}
     * @param create  must not be {@literal null} and must be valid
     */
    public Sprint(Project project, CreateSprint create) {
        this(create.getName(), create.getStartDate(), create.getEndDate(), Status.values()[create.getStatus()],
                create.getDescription(), project);
    }

    /**
     * Updates sprint with update request.
     * 
     * @param update must not be {@literal null} and must be valid
     */
    public void update(UpdateSprint update) {
        if (update.getName() != null) {
            setName(update.getName());
        }

        if (update.getDescription() != null) {
            setDescription(update.getDescription());
        }

        if (update.getStartDate() != null) {
            setStartDate(update.getStartDate());
        }

        if (update.getEndDate() != null) {
            setFinishDate(update.getEndDate());
        }

        if (update.getStatus() != null) {
            setStatus(update.getStatus());
        }

        if (getStartDate().after(getFinishDate())) {
            throw new FieldErrorException("date", "Start date must be before end date");
        }
    }

    @JsonIgnore
    public Status getStatusEnum() {
        return Status.values()[status];
    }

    /**
     * Sets name and trims it.
     * 
     * @param name must not be {@literal null}
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Sets description and trims it.
     * 
     * @param description must not be {@literal null}
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

    public void setStatus(Integer status) {
        this.status = status;
        if (status == 2) {
            this.setArchiveTasks(new ArrayList<>(this.getTasks().stream().map(t -> {
                t.setSprint(null);
                Set<Sprint> newArchive = new HashSet<>(t.getArchiveSprints());
                newArchive.add(this);
                t.setArchiveSprints(newArchive);
                return t;
            }).toList()));
            this.setTasks(new ArrayList<>());
        }
    }

}
