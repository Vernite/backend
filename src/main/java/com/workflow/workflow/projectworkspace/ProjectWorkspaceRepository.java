package com.workflow.workflow.projectworkspace;

import java.util.List;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.user.User;

import org.springframework.data.repository.CrudRepository;

public interface ProjectWorkspaceRepository extends CrudRepository<ProjectWorkspace, ProjectWorkspaceKey> {
    /**
     * It just works.
     * 
     * @param project not null pls.
     * @return function name is self-explanatory.
     */
    List<ProjectWorkspace> findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(Project project);

    List<ProjectWorkspace> findByWorkspaceUserInAndProject(Iterable<User> user, Project project);
}
