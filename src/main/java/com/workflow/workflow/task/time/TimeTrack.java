package com.workflow.workflow.task.time;

import java.util.Date;

import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;
import javax.persistence.MapsId;
import javax.validation.constraints.NotNull;

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

    private boolean edited = false;

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
    }

    /**
     * Updates the time spent on the task. If value changes, the edited flag is set
     * to true.
     * 
     * @param request the request to update the time spent on the task with.
     */
    public void update(@NotNull TimeTrackRequest request) {
        request.getTimeSpent().ifPresent(newTime -> {
            if (newTime != timeSpent) {
                timeSpent = newTime;
                edited = true;
            }
        });
    }

    /**
     * Toggles time tracking on the task.
     */
    public void toggle() {
        if (this.getStartTime() == null) {
            this.setStartTime(new Date());
        } else {
            this.timeSpent += (new Date().getTime() - this.getStartTime().getTime()) / 1000;
            this.setStartTime(null);
        }
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

    public boolean getEdited() {
        return edited;
    }

    public void setEdited(boolean edited) {
        this.edited = edited;
    }

    public boolean getEnabled() {
        return startTime != null;
    }
}
