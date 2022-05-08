package com.workflow.workflow.integration.git.github;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.task.Task;

import org.springframework.data.repository.CrudRepository;

public interface GitHubTaskRepository extends CrudRepository<GitHubTask, GitHubTaskKey> {
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
}
