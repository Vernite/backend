package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.project.Project;

import org.springframework.data.repository.CrudRepository;

public interface ProjectWorkspaceRepository extends CrudRepository<ProjectWorkspace, ProjectWorkspaceKey> {
    /**
     * This method finds pivot table entries for given project.
     * @param project - project which pivot table entries will be returned.
     * @return Iterable object with all pivot table entries for given project.
     */
    Iterable<ProjectWorkspace> findByProject(Project project);
}
