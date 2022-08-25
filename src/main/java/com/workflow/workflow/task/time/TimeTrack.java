package com.workflow.workflow.task.time;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

/**
 * Entity for tracking time spent on a task.
 */
@Entity
public class TimeTrack {
    @EmbeddedId
    @JsonUnwrapped
    private TimeTrackKey id;

    private long timeSpent = 0;

    @JsonIgnore
    private Date startTime;

    @JsonIgnore
    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    @JsonIgnore
    @MapsId("taskId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Task task;

    public TimeTrack() {
    }

    public TimeTrack(User user, Task task) {
        this.id = new TimeTrackKey(user, task);
        this.user = user;
        this.task = task;
        this.startTime = new Date();
    }

    public TimeTrackKey getId() {
        return id;
    }

    public void setId(TimeTrackKey id) {
        this.id = id;
    }

    public long getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(long timeSpent) {
        this.timeSpent = timeSpent;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
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
}
