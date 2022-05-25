package com.workflow.workflow.integration.git;

import java.util.ArrayList;
import java.util.List;

import com.workflow.workflow.integration.git.github.service.GitHubService;
import com.workflow.workflow.task.Task;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing task connected to any git service.
 */
@Service
public class GitTaskService {
    private final GitHubService gitHubService;

    public GitTaskService(GitHubService gitHubService) {
        this.gitHubService = gitHubService;
    }

    /**
     * Creates issue in appropriate git service.
     * 
     * @param task must not be {@literal null}.
     * @return future containing created issue.
     */
    public Flux<Issue> createIssue(Task task) {
        List<Mono<Issue>> futures = new ArrayList<>();
        if (gitHubService.isIntegrated(task.getStatus().getProject())) {
            futures.add(gitHubService.createIssue(task));
        }
        return Flux.concat(futures);
    }

    /**
     * Applies changes to issue in appropriate git service.
     * 
     * @param task must not be {@literal null}. Should have changed since last patch
     *             or since creation.
     * @return future containinge changed issue.
     */
    public Flux<Issue> patchIssue(Task task) {
        List<Mono<Issue>> futures = new ArrayList<>();
        if (gitHubService.isIntegrated(task)) {
            futures.add(gitHubService.patchIssue(task));
        }
        return Flux.concat(futures);
    }
}
