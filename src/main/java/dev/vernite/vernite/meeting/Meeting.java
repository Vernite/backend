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

package dev.vernite.vernite.meeting;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
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

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.FieldErrorException;

/**
 * Entity for representing meeting in project.
 */
@Data
@Entity
@NoArgsConstructor
public class Meeting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PositiveOrZero(message = "Id must be positive or zero")
    private long id;

    @Column(nullable = false, length = 50)
    @Size(min = 1, max = 50, message = "name must be shorter than 50 characters")
    @NotBlank(message = "name must contain at least one non-whitespace character")
    private String name;

    @Column(nullable = false, length = 1000)
    @NotNull(message = "description cannot be null")
    @Size(max = 1000, message = "description must be shorter than 1000 characters")
    private String description;

    @Column(length = 1000)
    @Size(max = 1000, message = "location must be shorter than 1000 characters")
    private String location;

    @Column(nullable = false)
    @NotNull(message = "start date cannot be null")
    private Date startDate;

    @Column(nullable = false)
    @NotNull(message = "end date cannot be null")
    private Date endDate;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @NotNull(message = "project connection must be set")
    private Project project;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(cascade = CascadeType.MERGE)
    @NotNull(message = "users connection must be set")
    private Set<User> participants = new HashSet<>();

    /**
     * Default constructor for meeting.
     * 
     * @param project     project to which meeting belongs
     * @param name        name of meeting
     * @param description description of meeting
     * @param startDate   start date of meeting
     * @param endDate     end date of meeting
     */
    public Meeting(Project project, String name, String description, Date startDate, Date endDate) {
        this.project = project;
        this.name = name.trim();
        this.description = description.trim();

        if (startDate.after(endDate)) {
            throw new FieldErrorException("startDate", "start date must be before end date");
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }

    /**
     * Constructor for meeting from create request.
     * 
     * @param project project to which meeting belongs
     * @param create  create request
     */
    public Meeting(Project project, CreateMeeting create) {
        this(project, create.getName(), create.getDescription(), create.getStartDate(), create.getEndDate());
        setLocation(create.getLocation());
    }

    /**
     * Update meeting from update request.
     * 
     * @param update update request
     */
    public void update(UpdateMeeting update) {
        if (update.getName() != null) {
            setName(update.getName());
        }

        if (update.getDescription() != null) {
            setDescription(update.getDescription());
        }

        if (update.getLocation() != null) {
            setLocation(update.getLocation());
        }

        if (update.getStartDate() != null) {
            setStartDate(update.getStartDate());
        }

        if (update.getEndDate() != null) {
            setEndDate(update.getEndDate());
        }

        if (getStartDate().after(getEndDate())) {
            throw new FieldErrorException("date", "Start date must be before end date");
        }
    }

    /**
     * Set name of meeting. Trimmed to remove whitespace.
     * 
     * @param name name of meeting
     */
    public void setName(String name) {
        this.name = name.trim();
    }

    /**
     * Set description of meeting. Trimmed to remove whitespace.
     * 
     * @param description description of meeting
     */
    public void setDescription(String description) {
        this.description = description.trim();
    }

    /**
     * Set location of meeting. Trimmed to remove whitespace.
     * 
     * @param location location of meeting
     */
    public void setLocation(String location) {
        this.location = location;
        if (this.location != null) {
            this.location = this.location.trim();
        }
    }

}
