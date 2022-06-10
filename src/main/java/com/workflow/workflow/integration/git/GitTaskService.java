package com.workflow.workflow.integration.git;

import java.util.List;

import com.workflow.workflow.integration.git.github.GitHubService;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for managing task connected to any git service.
 */
@Service
@Component
public class GitTaskService {
    @Autowired
    private GitHubService gitHubService;

    /**
     * Creates issue in appropriate git service.
     * 
     * @param task must not be {@literal null}.
     * @return Mono with created issue.
     */
    public Flux<Issue> createIssue(Task task) {
        return Flux.concat(List.of(gitHubService.createIssue(task)));
    }

    /**
     * Applies changes to issue in appropriate git service.
     * 
     * @param task must not be {@literal null}. Should have changed since last patch
     *             or since creation.
     * @return Mono with patched issue.
     */
    public Flux<Issue> patchIssue(Task task) {
        return Flux.concat(List.of(gitHubService.patchIssue(task), gitHubService.patchPullRequest(task)));
    }

    /**
     * Gets issues from git integrations for given project.
     * 
     * @param project must not be {@literal null}; must be entity from database.
     * @return Flux with issues.
     */
    public Flux<Issue> getIssues(Project project) {
        return Flux.concat(List.of(gitHubService.getIssues(project)));
    }

    /**
     * Connects given task with issue from git service.
     * 
     * @param task  must not be {@literal null}; must be entity from database.
     * @param issue must not be {@literal null}; must be returned by getIssues.
     * @return Mono with connected task.
     */
    public Mono<Issue> connectIssue(Task task, Issue issue) {
        if ("github".equals(issue.getService())) {
            return gitHubService.connectIssue(task, issue);
        }
        return Mono.empty();
    }

    /**
     * Deletes git issue connections for given task.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     */
    public void deleteIssue(Task task) {
        gitHubService.deleteIssue(task);
    }

    /**
     * Gets pull requests from git integrations for given project.
     * 
     * @param project must not be {@literal null}; must be entity from database.
     * @return Flux with pull requests.
     */
    public Flux<PullRequest> getPullRequests(Project project) {
        return Flux.concat(List.of(gitHubService.getPullRequests(project)));
    }

    /**
     * Connects given task with pull request from git service.
     * 
     * @param task        must not be {@literal null}; must be entity from database.
     * @param pullRequest must not be {@literal null}; must be returned by
     *                    getPullRequests.
     * @return Mono with connected pull request.
     */
    public Mono<PullRequest> connectPullRequest(Task task, PullRequest pullRequest) {
        if ("github".equals(pullRequest.getService())) {
            return gitHubService.connectPullRequest(task, pullRequest);
        }
        return Mono.empty();
    }

    /**
     * Deletes git pull request connections for given task.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     */
    public void deletePullRequest(Task task) {
        gitHubService.deletePullRequest(task);
    }
}
