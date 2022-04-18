package com.workflow.workflow.workspace;

import java.util.Optional;

import com.workflow.workflow.user.User;

import org.springframework.data.repository.CrudRepository;

public interface WorkspaceRepository extends CrudRepository<Workspace, Long> {
    Iterable<Workspace> findByUser(User user);

    Optional<Workspace> findByIdAndUser(Long id, User user);
}
