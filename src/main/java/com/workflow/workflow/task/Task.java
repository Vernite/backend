package com.workflow.workflow.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.db.Sprint;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne
    @JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "fk_task_sprint"))
    @OnDelete(action = OnDeleteAction.CASCADE)
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
    @Where(clause = "is_pull_request = 0")
    private List<GitHubTask> issues = new ArrayList<>();

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "task")
    @JsonIgnore
    @Where(clause = "is_pull_request = 1")
    private List<GitHubTask> pulls = new ArrayList<>();

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(mappedBy = "task")
    @JsonIgnore
    @Where(clause = "is_pull_request = 2")
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

    public Task() {
    }

    public Task(String name, String description, Status status, User user, int type) {
        this.name = name;
        this.description = description;
        this.status = status;
        this.user = user;
        this.type = type;
        this.createdAt = new Date();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
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
        return this.getStatus().getId();
    }

    public long getCreatedBy() {
        return this.getUser().getId();
    }

    public Long getAssigneeId() {
        return this.getAssignee() == null ? null : this.getAssignee().getId();
    }

    @JsonIgnore
    public void setState(String state) {
        if (state.equals("closed") && Boolean.FALSE.equals(getStatus().isFinal())) {
            for (Status newStatus : getStatus().getProject().getStatuses()) {
                if (Boolean.TRUE.equals(newStatus.isFinal())) {
                    setStatus(newStatus);
                    break;
                }
            }
        }
        if (state.equals("open") && Boolean.TRUE.equals(getStatus().isFinal())) {
            for (Status newStatus : getStatus().getProject().getStatuses()) {
                if (newStatus.isBegin()) {
                    setStatus(newStatus);
                    break;
                }
            }
        }
    }

    public String getIssue() {
        return !getIssues().isEmpty() ? getIssues().get(0).getLink() : null;
    }

    public String getPull() {
        return !getPulls().isEmpty() ? getPulls().get(0).getLink() : null;
    }

    public List<String> getMergedPullList() {
        return !getMergedPulls().isEmpty() ? getMergedPulls().stream().map(GitHubTask::getLink).collect(Collectors.toList()) : null;
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

    @Override
    public int hashCode() {
        return Objects.hash(getCreatedAt(), getDeadline(), getDescription(), getEstimatedDate(),
                getId(), this.getName(), this.getSprint(), this.getStatus(), this.getType(), this.getUser(), this.getAssignee());
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof Task))
            return false;
        Task other = (Task) obj;
        return Objects.equals(getCreatedAt(), other.getCreatedAt())
                && Objects.equals(getDeadline(), other.getDeadline())
                && Objects.equals(getDescription(), other.getDescription())
                && Objects.equals(getEstimatedDate(), other.getEstimatedDate())
                && getId() == other.getId()
                && Objects.equals(getName(), other.getName())
                && Objects.equals(getSprint(), other.getSprint())
                && Objects.equals(getStatus(), other.getStatus())
                && Objects.equals(getAssignee(), other.getAssignee())
                && getType() == other.getType() && Objects.equals(getUser(), other.getUser());
    }
}
