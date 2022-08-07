package com.workflow.workflow.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.ForeignKey;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.SoftDeleteEntity;

import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;

@Entity
@JsonInclude(Include.NON_NULL)
public class Task extends SoftDeleteEntity {
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
    @ManyToOne
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "fk_task_sprint"))
    private Sprint sprint;

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

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "task")
    @JsonIgnore
    @Where(clause = "is_pull_request = 0 and active is null")
    private List<GitHubTask> issues = new ArrayList<>();

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "task")
    @JsonIgnore
    @Where(clause = "is_pull_request = 1 and active is null")
    private List<GitHubTask> pulls = new ArrayList<>();

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "task")
    @JsonIgnore
    @Where(clause = "is_pull_request = 2 and active is null")
    private List<GitHubTask> mergedPulls = new ArrayList<>();

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

    public Sprint getSprint() {
        return sprint;
    }

    public void setSprint(Sprint sprint) {
        this.sprint = sprint;
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

    public void setType(int type) {
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
        return !getIssues().isEmpty() ? getIssues().get(0).toIssue() : null;
    }

    public PullRequest getPull() {
        return !getPulls().isEmpty() ? (PullRequest) getPulls().get(0).toIssue() : null;
    }

    public List<PullRequest> getMergedPullList() {
        return !getMergedPulls().isEmpty()
                ? getMergedPulls().stream().map(GitHubTask::toIssue).map(PullRequest.class::cast)
                        .collect(Collectors.toList())
                : null;
    }

    public List<GitHubTask> getIssues() {
        return issues;
    }

    public void setIssues(List<GitHubTask> issues) {
        this.issues = issues;
    }

    public List<GitHubTask> getPulls() {
        return pulls;
    }

    public void setPulls(List<GitHubTask> pulls) {
        this.pulls = pulls;
    }

    public List<GitHubTask> getMergedPulls() {
        return mergedPulls;
    }

    public void setMergedPulls(List<GitHubTask> mergedPulls) {
        this.mergedPulls = mergedPulls;
    }

    public Task getParentTask() {
        return parentTask;
    }

    public void setParentTask(Task superTask) {
        this.parentTask = superTask;
    }

    public Set<Task> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(Set<Task> subTasks) {
        this.subTasks = subTasks;
    }

    public Long getParentTaskId() {
        return this.parentTask != null ? this.parentTask.getId() : null;
    }

    public User getAssignee() {
        return assignee;
    }

    public void setAssignee(User assignee) {
        this.assignee = assignee;
    }

    public Long getSprintId() {
        return this.getSprint() == null ? null : this.getSprint().getNumber();
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }
}
