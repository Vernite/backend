package com.workflow.workflow.task.time;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.validation.constraints.NotNull;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

/**
 * Entity for tracking time spent on a task.
 */
@Entity
public class TimeTrack {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private boolean edited = false;

    @Column(nullable = false)
    private Date startDate;

    private Date endDate;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Task task;

    public TimeTrack() {
    }

    public TimeTrack(User user, Task task) {
        this.user = user;
        this.task = task;
        this.startDate = new Date();
    }

    /**
     * Updates the time spent on the task. If value changes, the edited flag is set
     * to true.
     * 
     * @param request the request to update the time spent on the task with.
     */
    public void update(@NotNull TimeTrackRequest request) {
        if (endDate == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Time track is not finished.");
        }
        
        edited = true;
        request.getStartDate().ifPresent(this::setStartDate);
        request.getEndDate().ifPresent(this::setEndDate);

        if (startDate.after(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Start date is after end date.");
        }
        if (endDate.after(new Date())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "End date is in the future.");
        }
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startTime) {
        this.startDate = startTime;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endTime) {
        this.endDate = endTime;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Task getTask() {
        return task;
    }

    public void setTask(Task task) {
        this.task = task;
    }

    public boolean getEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public long getTaskId() {
        return task.getNumber();
    }

    public long getProjectId() {
        return task.getStatus().getProject().getId();
    }

    public long getUserId() {
        return user.getId();
    }
}
