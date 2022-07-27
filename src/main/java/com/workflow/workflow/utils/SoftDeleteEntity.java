package com.workflow.workflow.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.persistence.MappedSuperclass;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Abstract class for representing database entities with soft delete
 * capability.
 */
@MappedSuperclass
public abstract class SoftDeleteEntity {
    @JsonIgnore
    private Date active;

    public Date getActive() {
        return active;
    }

    public void setActive(Date active) {
        this.active = active;
    }

    /**
     * Soft deletes entity for one week.
     */
    public void softDelete() {
        setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
    }
}
