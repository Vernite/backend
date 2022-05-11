package com.workflow.workflow.workspace;

import java.util.Optional;

import com.workflow.workflow.user.User;

import org.springframework.data.repository.CrudRepository;

public interface WorkspaceRepository extends CrudRepository<Workspace, Long> {
    /**
     * This method finds workspaces for given user.
     * 
     * @param user - user which workspaces wil be returned.
     * @return Iterable object with all workspaces for given user.
     */
    Iterable<Workspace> findByUser(User user);

    /**
     * This method looks for workspace with given id and belonging to given user.
     * 
     * @param id   - id of workspace.
     * @param user - user which workspace is being sought.
     * @return workspace with given id belonging to given user or nothing when not
     *         found.
     */
    Optional<Workspace> findByIdAndUser(Long id, User user);
}
