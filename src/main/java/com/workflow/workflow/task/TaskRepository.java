package com.workflow.workflow.task;

import java.util.Collection;

import com.workflow.workflow.status.Status;

import org.springframework.data.repository.CrudRepository;

public interface TaskRepository extends CrudRepository<Task, Long> {
    
    Collection<Task> findByStatus(Status status);
}
