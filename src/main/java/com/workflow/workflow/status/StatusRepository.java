package com.workflow.workflow.status;

import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.ObjectNotFoundException;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface StatusRepository extends SoftDeleteRepository<Status, Long> {
    /**
     * Finds a status by its number and project.
     * 
     * @param project the project.
     * @param number  the number of the status.
     * @return optional of the status.
     */
    Optional<Status> findByProjectAndNumberAndActiveNull(Project project, long number);

    /**
     * Finds a status by its number and project or throws error when not found.
     * 
     * @param project the project.
     * @param number  the number of the status.
     * @return the status.
     * @throws ObjectNotFoundException when not found.
     */
    default Status findByProjectAndNumberOrThrow(Project project, long number) {
        return findByProjectAndNumberAndActiveNull(project, number).orElseThrow(ObjectNotFoundException::new);
    }
}
