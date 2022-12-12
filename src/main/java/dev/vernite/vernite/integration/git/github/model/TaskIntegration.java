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

package dev.vernite.vernite.integration.git.github.model;

import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.task.Task;
import jakarta.annotation.Nullable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * Entity for representing GitHub task integration.
 */
@Data
@NoArgsConstructor
@Entity(name = "github_task_integration")
public class TaskIntegration {

    /**
     * Type of integrated resource.
     */
    public enum Type {
        ISSUE, PULL_REQUEST
    }

    @Valid
    @NotNull
    @EmbeddedId
    private TaskIntegrationId id;

    @NotNull
    @MapsId("taskId")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    private Task task;

    @NotNull
    @ToString.Exclude
    @MapsId("integrationId")
    @EqualsAndHashCode.Exclude
    @ManyToOne(optional = false)
    private ProjectIntegration projectIntegration;

    @Positive
    private long issueId;

    private boolean merged;

    @Nullable
    private String branch;

    /**
     * Default constructor for task integration
     * 
     * @param task        task to integrate
     * @param integration integration to integrate with
     * @param issueId     issue id
     * @param type        type of integrated resource
     */
    public TaskIntegration(Task task, ProjectIntegration integration, long issueId, Type type) {
        this.id = new TaskIntegrationId(task.getId(), integration.getId(), type.ordinal());
        this.task = task;
        this.projectIntegration = integration;
        this.issueId = issueId;
        this.merged = false;
    }

    /**
     * @return link to the issue
     */
    public String link() {
        String repositoryOwner = projectIntegration.getRepositoryOwner();
        String repositoryName = projectIntegration.getRepositoryName();
        String type = getId().getType() == Type.ISSUE.ordinal() ? "issues" : "pull";
        return String.format("https://github.com/%s/%s/%s/%d", repositoryOwner, repositoryName, type, getIssueId());
    }

    /**
     * Converts this task integration to issue.
     * 
     * @return issue
     */
    public Issue toIssue() {
        return new Issue(getIssueId(), link(), getTask().getName(), getTask().getDescription(), "github");
    }

    public PullRequest toPullRequest() {
        return new PullRequest(getIssueId(), link(), getTask().getName(), getTask().getDescription(), "github",
                getBranch());
    }

}
