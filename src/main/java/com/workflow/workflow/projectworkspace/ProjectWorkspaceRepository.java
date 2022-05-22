package com.workflow.workflow.projectworkspace;

import java.util.List;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.workspace.Workspace;

import org.springframework.data.repository.CrudRepository;

public interface ProjectWorkspaceRepository extends CrudRepository<ProjectWorkspace, ProjectWorkspaceKey> {
    /**
     * This methods finds all pivot table entry for given workspace.
     * 
     * @param workspace - workspace which pivot entries will be returned.
     * @return Pivot table entries list for workspace.
     */
    List<ProjectWorkspace> findByWorkspace(Workspace workspace);

    /**
     * It just works.
     * 
     * @param project not null pls.
     * @return function name is self-explanatory.
     */
    List<ProjectWorkspace> findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(Project project);
}
