package com.workflow.workflow.integration.git;

import java.util.ArrayList;
import java.util.List;

import com.workflow.workflow.integration.git.github.GitHubService;
import com.workflow.workflow.project.Project;
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
     * @return future containing created issues.
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
     * @return future containing changed issues.
     */
    public Flux<Issue> patchIssue(Task task) {
        List<Mono<Issue>> futures = new ArrayList<>();
        if (gitHubService.isIntegrated(task)) {
            futures.add(gitHubService.patchIssue(task));
        }
        return Flux.concat(futures);
    }

    /**
     * Gets issues from git integrations for given project.
     * 
     * @param project must not be {@literal null}; must be entity from database.
     * @return future containing list of issues.
     */
    public Flux<Issue> getIssues(Project project) {
        List<Flux<Issue>> futures = new ArrayList<>();
        if (gitHubService.isIntegrated(project)) {
            futures.add(gitHubService.getIssues(project));
        }
        return Flux.concat(futures);
    }

    /**
     * Connects given task with issue from git service.
     * 
     * @param task  must not be {@literal null}; must be entity from database.
     * @param issue must not be {@literal null}; must be returned by getIssues.
     * @return future containing connected issue.
     */
    public Mono<Issue> connectIssue(Task task, Issue issue) {
        if ("github".equals(issue.getService()) && gitHubService.isIntegrated(task)) {
            return gitHubService.connectIssue(task, issue);
        }
        return Mono.empty();
    }

    /**
     * Deletes github connections for given task.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return future containing nothing.
     */
    public void deleteIssue(Task task) {
        if (gitHubService.isIntegrated(task)) {
            gitHubService.deleteIssue(task);
        }
    }
}
