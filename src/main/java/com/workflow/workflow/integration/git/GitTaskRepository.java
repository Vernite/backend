package com.workflow.workflow.integration.git;

import org.springframework.data.repository.CrudRepository;

public interface GitTaskRepository extends CrudRepository<GitTask, GitTaskKey> {}
