package com.workflow.workflow.projectworkspace;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.Workspace;

import org.springframework.data.repository.CrudRepository;

public interface ProjectWorkspaceRepository extends CrudRepository<ProjectWorkspace, ProjectWorkspaceKey> {
    /**
     * This method finds pivot table entries for given project.
     * @param project - project which pivot table entries will be returned.
     * @return Iterable object with all pivot table entries for given project.
     */
    Iterable<ProjectWorkspace> findByProject(Project project);
    
    /**
     * This method finds pivot table entry describing user membership in project.
     * @param project - project which pivot table entry will be returned.
     * @param user - user which workspace is element of pivot table entry.
     * @return Pivot table entry connecting user workspace with project.
     */
    Optional<ProjectWorkspace> findByProjectAndWorkspaceUser(Project project, User user);

    /**
     * This methods finds all pivot table entry for given workspace.
     * @param workspace - workspace which pivot entries will be returned.
     * @return Pivot table entries list for workspace.
     */
    List<ProjectWorkspace> findByWorkspace(Workspace workspace);
}
