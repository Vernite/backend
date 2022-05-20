package com.workflow.workflow.counter;

import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.CrudRepository;

/**
 * Interface for operations on CounterSequence entity.
 */
public interface CounterSequenceRepository extends CrudRepository<CounterSequence, Long> {

    /**
     * Runs transaction which adds one to counter sequence with given id. It is used
     * for safe database incrementing to prevent race condition.
     * 
     * @param counterId should not be {@literal null}.
     * @return counter sequence value after transaction.
     */
    @Procedure("counter_increment")
    long getIncrementCounter(long counterId);
}
