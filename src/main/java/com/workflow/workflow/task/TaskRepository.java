package com.workflow.workflow.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.ObjectNotFoundException;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface TaskRepository extends SoftDeleteRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    /**
     * Finds a task by its number and project.
     * 
     * @param project the project.
     * @param number  the number of the task.
     * @return optional of the task.
     */
    Optional<Task> findByStatusProjectAndNumberAndActiveNull(Project project, long number);

    /**
     * Finds a task by its number and project or throws error when not found.
     * 
     * @param project the project.
     * @param number  the number of the task.
     * @return the task.
     * @throws ObjectNotFoundException when not found.
     */
    default Task findByProjectAndNumberOrThrow(Project project, long number) {
        return findByStatusProjectAndNumberAndActiveNull(project, number).orElseThrow(ObjectNotFoundException::new);
    }

    /**
     * Finds tasks by specification.
     * 
     * @param spec the specification.
     * @return the tasks ordered by name and number.
     */
    default List<Task> findAllOrdered(Specification<Task> spec) {
        return findAll(spec, Sort.by(Direction.ASC, "name", "number"));
    }
}
