package com.workflow.workflow.sprint;

import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.ObjectNotFoundException;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface SprintRepository extends SoftDeleteRepository<Sprint, Long> {
    /**
     * Finds a sprint by its number and project.
     * 
     * @param project the project.
     * @param number  the number of the sprint.
     * @return optional of the sprint.
     */
    Optional<Sprint> findByProjectAndNumberAndActiveNull(Project project, long number);

    /**
     * Finds a sprint by its number and project or throws error when not found.
     * 
     * @param project the project.
     * @param number  the number of the sprint.
     * @return the sprint.
     * @throws ObjectNotFoundException when not found.
     */
    default Sprint findByProjectAndNumberOrThrow(Project project, long number) {
        return findByProjectAndNumberAndActiveNull(project, number).orElseThrow(ObjectNotFoundException::new);
    }
}
