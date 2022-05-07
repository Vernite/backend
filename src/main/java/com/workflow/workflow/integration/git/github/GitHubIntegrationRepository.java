package com.workflow.workflow.integration.git.github;

import java.util.Optional;

import com.workflow.workflow.project.Project;

import org.springframework.data.repository.CrudRepository;

public interface GitHubIntegrationRepository extends CrudRepository<GitHubIntegration, Long> {
    /**
     * This method finds integration with GitHub for given project.
     * @param project - project for which integration will be found.
     * @return Integration for given project.
     */
    Optional<GitHubIntegration> findByProject(Project project);
}
