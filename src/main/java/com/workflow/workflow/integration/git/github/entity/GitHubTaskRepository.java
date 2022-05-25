package com.workflow.workflow.integration.git.github.entity;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.task.Task;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface GitHubTaskRepository extends SoftDeleteRepository<GitHubTask, GitHubTaskKey> {
    /**
     * This method finds GitHub issue connection for task.
     * 
     * @param task - Task which connection is looked for.
     * @return Optional with connection to GitHub issue; empty when there is not
     *         any.
     */
    Optional<GitHubTask> findByTask(Task task);

    /**
     * This method finds GitHub issues connection for integration.
     * 
     * @param integration - Integration which connection is looked for.
     * @return List of all task associeted with integration.
     */
    List<GitHubTask> findByGitHubIntegration(GitHubIntegration integration);

    /**
     * This method finds GitHub issue connections for integration and issue id.
     * 
     * @param issueId     - id of github issue.
     * @param integration - integration with github.
     * @return List of connection between issue and task.
     */
    List<GitHubTask> findByIssueIdAndGitHubIntegration(long issueId, GitHubIntegration integration);
}
