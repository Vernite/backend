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

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskIssue;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskPull;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.task.time.TimeTrack;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

@Entity
@JsonInclude(Include.NON_NULL)
public class Task extends SoftDeleteEntity {

    public enum TaskType {
        TASK, USER_STORY, ISSUE, EPIC, SUBTASK;

        public boolean isValidParent(TaskType parent) {
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @JsonProperty("id")
    @Column(nullable = false)
    private long number;

    @Column(nullable = false, length = 50)
    private String name;

    @JsonIgnore
    @ManyToMany(cascade = CascadeType.MERGE)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "fk_task_sprint"))
    private Set<Sprint> sprints = Set.of();

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Date createdAt;

    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_status"))
    private Status status;

    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_task_user"))
    private User user;

    @JsonIgnore
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "assignee", nullable = true, foreignKey = @ForeignKey(name = "fk_task_assignee"))
    private User assignee;

    @Column(nullable = false)
    private int type;

    @JsonIgnore
    @OneToOne(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GitHubTaskIssue issueTask;

    @JsonIgnore
    @OneToOne(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private GitHubTaskPull pullTask;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(nullable = true)
    private Task parentTask;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "parentTask")
    @Where(clause = "active is null")
    @OrderBy("name, id")
    private Set<Task> subTasks = new HashSet<>();

    private Date deadline;
    private Date estimatedDate;

    @Column(nullable = false)
    private String priority;

    @OneToMany(mappedBy = "task")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private List<TimeTrack> timeTracks = new ArrayList<>();

    @Column(nullable = false)
    private long storyPoints;

    public Task() {
    }

    public Task(long id, String name, String description, Status status, User user, int type, String priority) {
        this.number = id;
        this.name = name;
        this.description = description;
        this.status = status;
        this.user = user;
        this.type = type;
        this.priority = priority;
        this.createdAt = new Date();
    }

    public Task(long id, String name, String description, Status status, User user, int type) {
        this(id, name, description, status, user, type, "low");
    }

    /**
     * Updates task with non-empty request fields.
     * 
     * @param request must not be {@literal null}. When fields are not present in
     *                request, they are not updated.
     */
    public void update(@NotNull TaskRequest request) {
        request.getDeadline().ifPresent(this::setDeadline);
        request.getEstimatedDate().ifPresent(this::setEstimatedDate);
        request.getPriority().ifPresent(this::setPriority);
        request.getName().ifPresent(this::setName);
        request.getDescription().ifPresent(this::setDescription);
        request.getType().ifPresent(this::setType);
        request.getStoryPoints().ifPresent(this::setStoryPoints);
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getNumber() {
        return number;
    }

    public void setNumber(long number) {
        this.number = number;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Sprint> getSprints() {
        return sprints;
    }

    public void setSprints(Set<Sprint> sprints) {
        this.sprints = sprints;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public int getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public Date getEstimatedDate() {
        return estimatedDate;
    }

    public void setEstimatedDate(Date estimatedDate) {
        this.estimatedDate = estimatedDate;
    }

    public long getStatusId() {
        return this.getStatus().getNumber();
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

    public Issue getIssue() {
        return getIssueTask() != null ? getIssueTask().toIssue() : null;
    }

    public PullRequest getPull() {
        return getPullTask() != null ? getPullTask().toPull() : null;
    }

    public GitHubTaskIssue getIssueTask() {
        return issueTask;
    }

    public void setIssueTask(GitHubTaskIssue issues) {
        this.issueTask = issues;
    }

    public GitHubTaskPull getPullTask() {
        return pullTask;
    }

    public void setPullTask(GitHubTaskPull pulls) {
        this.pullTask = pulls;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task superTask) {
        this.parentTask = superTask;
    }

    public Set<Task> getSubTasks() {
        return type == TaskType.EPIC.ordinal() ? Set.of() : subTasks;
    }

    public void setSubTasks(Set<Task> subTasks) {
        this.subTasks = subTasks;
    }

    public Long getParentTaskId() {
        return this.parentTask != null ? this.parentTask.getNumber() : null;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    public List<Long> getSprintIds() {
        return this.getSprints().stream().map(Sprint::getNumber).toList();
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public List<TimeTrack> getTimeTracks() {
        return timeTracks;
    }

    public void setTimeTracks(List<TimeTrack> timeTracks) {
        this.timeTracks = timeTracks;
    }

    public long getStoryPoints() {
        return storyPoints;
    }

    public void setStoryPoints(Long storyPoints) {
        this.storyPoints = storyPoints;
    }
}
