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
     * Handle issue action for a given task.
     * 
     * @param action the action to handle.
     * @param task   the task to handle.
     * @return flux with changed issues.
     */
    public Flux<Issue> handleIssueAction(IssueAction action, Task task) {
        switch (action) {
            case ATTACH:
                return connectIssue(task, action.getIssue()).flux();
            case CREATE:
                return createIssue(task);
            case DETACH:
                deleteIssue(task);
        }
        return Flux.empty();
    }

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
     * Handles pull action for a given task.
     * 
     * @param action the action to handle.
     * @param task   the task to handle.
     * @return flux with changed pull requests.
     */
    public Flux<PullRequest> handlePullAction(PullAction action, Task task) {
        if (action.equals(PullAction.ATTACH)) {
            return connectPullRequest(task, action.getPullRequest()).flux();
        } else {
            deletePullRequest(task);
        }
        return Flux.empty();
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
