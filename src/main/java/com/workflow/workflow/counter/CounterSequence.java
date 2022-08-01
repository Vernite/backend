package com.workflow.workflow.counter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

/**
 * Entity for storing sequence counters for generation of unique ids.
 */
@Entity
public class CounterSequence {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long counterValue = 0;

    public CounterSequence() {
    }

    public CounterSequence(long counterValue) {
        this.counterValue = counterValue;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getCounterValue() {
        return counterValue;
    }

    public void setCounterValue(long counterValue) {
        this.counterValue = counterValue;
    }
}
