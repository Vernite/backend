package com.workflow.workflow.task.time;

import java.io.Serializable;

import javax.persistence.Embeddable;

import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

/**
 * Composite key for time track. Composed of of task id and user id.
 */
@Embeddable
public class TimeTrackKey implements Serializable, Comparable<TimeTrackKey> {
    private long userId;
    private long taskId;

    public TimeTrackKey() {
    }

    public TimeTrackKey(User user, Task task) {
        this.userId = user.getId();
        this.taskId = task.getId();
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getTaskId() {
        return taskId;
    }

    public void setTaskId(long taskId) {
        this.taskId = taskId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(taskId);
        hash = prime * hash + Long.hashCode(userId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        TimeTrackKey other = (TimeTrackKey) obj;
        return taskId == other.taskId && userId == other.userId;
    }

    @Override
    public int compareTo(TimeTrackKey other) {
        return taskId == other.taskId ? Long.compare(userId, other.userId) : Long.compare(taskId, other.taskId);
    }
}
