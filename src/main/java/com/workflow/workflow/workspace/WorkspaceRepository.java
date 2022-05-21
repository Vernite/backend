package com.workflow.workflow.workspace;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.entity.Workspace;
import com.workflow.workflow.workspace.entity.WorkspaceKey;

import org.springframework.data.repository.CrudRepository;

public interface WorkspaceRepository extends CrudRepository<Workspace, WorkspaceKey> {
    /**
     * This method finds workspaces for given user.
     * 
     * @param user - user which workspaces wil be returned.
     * @return List object with all workspaces for given user.
     */
    List<Workspace> findByUser(User user);

    /**
     * This method looks for workspace with given id and belonging to given user.
     * 
     * @param id   - id of workspace.
     * @param user - user which workspace is being sought.
     * @return workspace with given id belonging to given user or nothing when not
     *         found.
     */
    Optional<Workspace> findByIdAndUser(WorkspaceKey id, User user);
}
