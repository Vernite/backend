package com.workflow.workflow.task;

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
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.db.Sprint;
import com.workflow.workflow.integration.git.github.GitHubTask;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.user.User;

@Entity
@JsonInclude(Include.NON_NULL)
public class Task {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @ManyToOne
    @JoinColumn(name = "sprint_id", foreignKey = @ForeignKey(name = "fk_task_sprint"))
    private Sprint sprint;

    @Lob
    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Date createdAt;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "status_id", nullable = false, foreignKey = @ForeignKey(name = "fk_task_status"))
    private Status status;

    @JsonIgnore
    @ManyToOne
    @JoinColumn(name = "created_by", nullable = false, foreignKey = @ForeignKey(name = "fk_task_user"))
    private User user;

    @Column(nullable = false)
    private int type;

    @OneToOne(mappedBy = "task", cascade = CascadeType.ALL)
    private GitHubTask gitHubTask;

    @ManyToOne
    @JoinColumn(nullable = true)
    private Task parentTask;

    @OneToMany(mappedBy = "parentTask")
    private Set<Task> subTasks = new HashSet<>();

    private Date deadline;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
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

    public long getStatusId() {
        return this.getStatus().getId();
    }

    public long getCreatedBy() {
        return this.getUser().getId();
    }

    @JsonIgnore
    public void setState(String state) {
        if (state.equals("closed") && Boolean.FALSE.equals(getStatus().isFinal())) {
            setStatus(getStatus().getProject().getStatuses().stream().filter(s -> Boolean.TRUE.equals(s.isFinal()))
                    .findFirst().orElseThrow());
        }
        if (state.equals("open") && Boolean.TRUE.equals(getStatus().isFinal())) {
            List<Status> statuses = new ArrayList<>(getStatus().getProject().getStatuses());
            statuses.sort((first, second) -> {
                if (Boolean.TRUE.equals(first.isFinal())) {
                    return 1;
                }
                if (Boolean.TRUE.equals(second.isFinal())) {
                    return -1;
                }
                return first.getOrdinal() > second.getOrdinal() ? 1 : -1;
            });
            setStatus(statuses.get(0));
        }
    }

    public String getIssue() {
        return gitHubTask != null ? gitHubTask.getLink() : null;
    }

    public void setGitHubTask(GitHubTask gitHubTask) {
        this.gitHubTask = gitHubTask;
    }

    @JsonIgnore
    public Task getParentTask() {
        return parentTask;
    }

    @JsonIgnore
    public void setParentTask(Task superTask) {
        this.parentTask = superTask;
    }

    public Set<Task> getSubTasks() {
        return subTasks;
    }

    public void setSubTasks(Set<Task> subTasks) {
        this.subTasks = subTasks;
    }

    public Long getSuperTaskId() {
        return this.parentTask != null ? this.parentTask.getId() : null;
    }
}
