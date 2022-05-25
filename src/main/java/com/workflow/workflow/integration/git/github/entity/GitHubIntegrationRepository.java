package com.workflow.workflow.integration.git.github.entity;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface GitHubIntegrationRepository extends SoftDeleteRepository<GitHubIntegration, Long> {
    /**
     * This method finds integration with GitHub for given project.
     * 
     * @param project - project for which integration will be found.
     * @return Integration for given project.
     */
    Optional<GitHubIntegration> findByProject(Project project);

    /**
     * This method finds all integrations for given GitHub account.
     * 
     * @param installation - installation for which integrations will be found.
     * @return Integrations associated with given installation.
     */
    List<GitHubIntegration> findByInstallation(GitHubInstallation installation);

    /**
     * This method finds all integrations for repository full name.
     * 
     * @param respositoryFullName - full name of GitHub repostory to which
     *                            integrations will be returned.
     * @return Integrations associated with repository with given name.
     */
    List<GitHubIntegration> findByRepositoryFullName(String respositoryFullName);
}
