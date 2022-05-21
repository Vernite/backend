package com.workflow.workflow.workspace.entity;

import java.io.Serializable;

import javax.persistence.Embeddable;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.workflow.workflow.user.User;

/**
 * Composite key for workspace. Composed of of id and user id.
 */
@Embeddable
public class WorkspaceKey implements Serializable, Comparable<WorkspaceKey> {
    private long id;

    @JsonIgnore
    private long userId;

    public WorkspaceKey() {
    }

    WorkspaceKey(long id) {
        this.id = id;
    }

    public WorkspaceKey(long id, User user) {
        this.id = id;
        this.userId = user.getId();
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int hash = prime + Long.hashCode(id);
        hash = prime * hash + Long.hashCode(userId);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        WorkspaceKey other = (WorkspaceKey) obj;
        return id == other.id && userId == other.userId;
    }

    @Override
    public int compareTo(WorkspaceKey other) {
        return userId == other.userId ? Long.compare(id, other.id) : Long.compare(userId, other.userId);
    }
}
