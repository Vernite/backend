package com.workflow.workflow.integration.git.github.entity.task;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.NoRepositoryBean;

import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.task.Task;

@NoRepositoryBean
public interface GitHubTaskRepository<T, K> extends JpaRepository<T, K> {
    /**
     * This method finds GitHub issue / pull request connection for task.
     * 
     * @param task which connection is looked for.
     * @return Optional with connection to GitHub issue / pull request; empty when
     *         there is not any.
     */
    Optional<T> findByTask(Task task);

    /**
     * This method finds GitHub issues connection for integration.
     * 
     * @param integration which connection is looked for.
     * @return List of all task associeted with integration.
     */
    List<T> findByGitHubIntegration(GitHubIntegration integration);

    /**
     * This method finds GitHub issue connections for integration and issue id.
     * 
     * @param issueId     id of github issue.
     * @param integration integration with github.
     * @return List of connection between issue and task.
     */
    List<T> findByIssueIdAndGitHubIntegration(long issueId, GitHubIntegration integration);
}
