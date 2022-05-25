package com.workflow.workflow.integration.git.github.entity;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.user.User;

import org.springframework.data.repository.CrudRepository;

public interface GitHubInstallationRepository extends CrudRepository<GitHubInstallation, Long> {
    /**
     * This method finds all associeted GitHub istallations for given user.
     * 
     * @param user - user which GitHub installations will be returned;
     * @return List with all users GitHub installations.
     */
    List<GitHubInstallation> findByUser(User user);

    /**
     * This method find installation for given user with given id.
     * 
     * @param id   - id of installation to find.
     * @param user - user which installation will be returned.
     * @return Installation with given id and user.
     */
    Optional<GitHubInstallation> findByIdAndUser(long id, User user);

    /**
     * This method finds installation by installation id.
     * 
     * @param installationId - id of installation to find.
     * @return Installation with given installation id.
     */
    Optional<GitHubInstallation> findByInstallationId(long installationId);
}
