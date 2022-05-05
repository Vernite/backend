package com.workflow.workflow.integration.git.github;

import org.springframework.data.repository.CrudRepository;

public interface GitHubTaskRepository extends CrudRepository<GitHubTask, GitHubTaskKey> {}
