/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
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

package dev.vernite.vernite.task;

import java.util.Date;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import dev.vernite.vernite.integration.git.IssueAction;
import dev.vernite.vernite.integration.git.IssueActionDeserializer;
import dev.vernite.vernite.integration.git.PullAction;
import dev.vernite.vernite.integration.git.PullActionDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Class containing information needed to create new task entity.
 * Has required constraints annotated using Java Bean Validation.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateTask {

    /**
     * The name of the task.
     */
    @NotBlank(message = "Task name cannot be blank")
    @Size(min = 1, max = 100, message = "Task name must be between 1 and 100 characters")
    private String name;

    /**
     * The description of the task.
     */
    @NotNull(message = "Task description cannot be null")
    @Size(max = 1000, message = "Task description must be less than 1000 characters")
    private String description;

    /**
     * The id of the status to which the task belongs.
     */
    @NotNull(message = "Task status id cannot be null")
    @Positive(message = "Task status id must be positive")
    private Long statusId;

    /**
     * The id of the assignee to which the task belongs.
     */
    @Positive(message = "Task assignee id must be positive")
    private Long assigneeId;

    /**
     * The type of the task.
     */
    @NotNull(message = "Task type cannot be null")
    @PositiveOrZero(message = "Task type must be positive or zero")
    private Integer type;

    /**
     * The priority of the task.
     */
    @NotBlank(message = "Task priority cannot be blank")
    @Size(min = 1, max = 100, message = "Task priority must be between 1 and 100 characters")
    private String priority;

    /**
     * The estimated date of the task.
     */
    private Date estimatedDate;

    /**
     * The due date of the task.
     */
    private Date deadline;

    /**
     * The id of the parent task to which the task belongs.
     */
    @Positive(message = "Task parent task id must be positive")
    private Long parentTaskId;

    /**
     * Instead of word 'attach' send issue object received from other endpoint:
     * /project/{id}/integration/git/issue
     */
    @JsonDeserialize(using = IssueActionDeserializer.class)
    private IssueAction issue;

    /**
     * Instead of word 'attach' send pull object received from other endpoint:
     * /project/{id}/integration/git/pull
     */
    @JsonDeserialize(using = PullActionDeserializer.class)
    private PullAction pull;

    /**
     * The id of the sprint to which the task belongs.
     */
    @Positive(message = "Task sprint id must be positive")
    private Long sprintId;

    /**
     * The amount of story points assigned to the task.
     */
    @PositiveOrZero(message = "Task story points must be positive or zero")
    private Long storyPoints;

    /**
     * The id of the release to which the task belongs.
     */
    @Positive(message = "Task release id must be positive")
    private Long releaseId;

    /**
     * Constructor for creating task with only required fields.
     * 
     * @param name        name of the task
     * @param description description of the task
     * @param statusId    id of the status to which the task belongs
     * @param type        type of the task
     * @param priority    priority of the task
     */
    public CreateTask(String name, String description, Long statusId, Integer type, String priority) {
        this(name, description, statusId, null, type, priority, null, null, null, null, null, null, null, null);
    }

}
