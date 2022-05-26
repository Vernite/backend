package com.workflow.workflow.task;

import java.util.Collection;
import java.util.List;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface TaskRepository extends SoftDeleteRepository<Task, Long> {
    
    Collection<Task> findByStatus(Status status);

    List<Task> findByStatusProjectAndActiveNullOrderByNameAscIdAsc(Project project);
}
