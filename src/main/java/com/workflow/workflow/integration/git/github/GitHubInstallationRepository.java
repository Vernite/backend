package com.workflow.workflow.integration.git.github;

import java.util.List;

import com.workflow.workflow.user.User;

import org.springframework.data.repository.CrudRepository;

public interface GitHubInstallationRepository extends CrudRepository<GitHubInstallation, Long> {
    List<GitHubInstallation> findByUser(User user);
}
