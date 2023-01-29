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

package dev.vernite.vernite.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.model.TaskIntegration;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.task.comment.Comment;
import dev.vernite.vernite.task.time.TimeTrack;
import dev.vernite.vernite.user.User;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Entity
@NoArgsConstructor
@JsonInclude(Include.NON_NULL)
@EntityListeners(TaskListener.class)
public class Task {

    public enum Type {
        TASK, USER_STORY, ISSUE, EPIC, SUBTASK;

        public boolean isValidParent(Type parent) {
            if (this == parent) {
                return false;
            }
            switch (this) {
                case EPIC:
                    return false;
                case TASK:
                    return parent == EPIC;
                case USER_STORY:
                    return parent == EPIC;
                case ISSUE:
                    return parent == EPIC || parent == TASK;
                case SUBTASK:
                    return parent != EPIC;
                default:
                    return false;
            }
        }
    }

    @Id
    @JsonIgnore
    @PositiveOrZero
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Positive
    @JsonProperty("id")
    @Column(nullable = false)
    private long number;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Sprint sprint;

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToMany(cascade = CascadeType.MERGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Set<Sprint> archiveSprints = new HashSet<>();

    @NotNull
    @Column(nullable = false, length = 1000)
    private String description;

    @NotNull
    @Column(nullable = false)
    private Date createdAt;

    @NotNull
    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Status status;

    @NotNull
    @JsonIgnore
    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by", nullable = false)
    private User user;

    @JsonIgnore
    @ManyToOne
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(nullable = true)
    private User assignee;

    @Column(nullable = false)
    private int type;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Task parentTask;

    @NotNull
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "parentTask")
    @OrderBy("name, id")
    private List<Task> subTasks = new ArrayList<>();

    private Date deadline;
    private Date estimatedDate;

    @NotNull
    @Column(nullable = false)
    private String priority;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @NotNull
    @OneToMany(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<TimeTrack> timeTracks = new ArrayList<>();

    @Column(nullable = false)
    private long storyPoints;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne
    @JsonIgnore
    private Release release;

    @NotNull
    private Date lastUpdated;

    @NotNull
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @JsonIgnore
    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<Comment> comments = new ArrayList<>();

    @NotNull
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToMany(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<TaskIntegration> gitHubTaskIntegrations = new ArrayList<>();

    /**
     * Default constructor for Task.
     * 
     * @param id          ID of the task
     * @param name        name of the task
     * @param description description of the task
     * @param status      status of the task
     * @param user        user who created the task
     * @param type        type of the task
     * @param priority    priority of the task
     */
    public Task(long id, String name, String description, Status status, User user, int type, String priority) {
        this.number = id;
        setName(name);
        setDescription(description);
        this.status = status;
        this.user = user;
        this.type = type;
        this.priority = priority;
        this.createdAt = new Date();
    }

    /**
     * Constructor for Task from request.
     * 
     * @param id     ID of the task
     * @param status status of the task
     * @param user   user who created the task
     * @param create request for creating task
     */
    public Task(long id, Status status, User user, CreateTask create) {
        this(id, create.getName(), create.getDescription(), status, user, create.getType(), create.getPriority());
        this.deadline = create.getDeadline();
        this.estimatedDate = create.getEstimatedDate();

        if (create.getStoryPoints() != null) {
            this.storyPoints = create.getStoryPoints();
        }
    }

    @Deprecated
    public Task(long id, String name, String description, Status status, User user, int type) {
        this(id, name, description, status, user, type, "low");
    }

    /**
     * Updates task with non-empty request fields.
     * 
     * @param update must not be {@literal null}. When fields are not present in
     *               request, they are not updated.
     */
    public void update(UpdateTask update) {
        if (update.getDeadline() != null) {
            this.setDeadline(update.getDeadline());
        }
        if (update.getEstimatedDate() != null) {
            this.setEstimatedDate(update.getEstimatedDate());
        }
        if (update.getPriority() != null) {
            this.setPriority(update.getPriority());
        }
        if (update.getName() != null) {
            this.setName(update.getName());
        }
        if (update.getDescription() != null) {
            this.setDescription(update.getDescription());
        }
        if (update.getType() != null) {
            this.setType(update.getType());
        }
        if (update.getStoryPoints() != null) {
            this.setStoryPoints(update.getStoryPoints());
        }
    }

    public long getStatusId() {
        return this.getStatus().getId();
    }

    public long getCreatedBy() {
        return this.getUser().getId();
    }

    public Long getAssigneeId() {
        return this.getAssignee() == null ? null : this.getAssignee().getId();
    }

    public void changeStatus(boolean isOpen) {
        if (isOpen && !getStatus().isBegin()) {
            for (Status newStatus : getStatus().getProject().getStatuses()) {
                if (newStatus.isBegin()) {
                    setStatus(newStatus);
                    break;
                }
            }
        } else if (!isOpen && !getStatus().isFinal()) {
            for (Status newStatus : getStatus().getProject().getStatuses()) {
                if (newStatus.isFinal()) {
                    setStatus(newStatus);
                    break;
                }
            }
        }
    }

    public List<Task> getSubTasks() {
        return type == Type.EPIC.ordinal() ? List.of() : subTasks;
    }

    public Long getParentTaskId() {
        return this.parentTask != null ? this.parentTask.getNumber() : null;
    }

    public Long getReleaseId() {
        return this.release != null ? this.release.getId() : null;
    }

    public Long getSprintId() {
        return this.getSprint() == null ? null : this.getSprint().getId();
    }

    @JsonProperty(access = Access.READ_ONLY)
    public List<Long> getArchivedSprintIds() {
        return this.getArchiveSprints().stream().map(Sprint::getId).toList();
    }

    @PreUpdate
    @PrePersist
    private void updateDate() {
        this.setLastUpdated(new Date());
    }

    public PullRequest getPull() {
        for (var integration : getGitHubTaskIntegrations()) {
            if (integration.getId().getType() == TaskIntegration.Type.PULL_REQUEST.ordinal()) {
                var pull = new PullRequest(integration.getIssueId(), integration.link(), getName(), getDescription(),
                        "github", integration.getBranch());
                if (integration.isMerged()) {
                    pull.setState("merged");
                }
                return pull;
            }
        }
        return null;
    }

    public Issue getIssue() {
        for (var integration : getGitHubTaskIntegrations()) {
            if (integration.getId().getType() == TaskIntegration.Type.ISSUE.ordinal()) {
                return integration.toIssue();
            }
        }
        return null;
    }

    public long getProjectId() {
        return this.getStatus().getProject().getId();
    }
}
