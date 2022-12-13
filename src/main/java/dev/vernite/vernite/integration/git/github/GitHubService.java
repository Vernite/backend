/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.integration.git.github;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.apache.hc.core5.net.URIBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import dev.vernite.vernite.common.exception.ExternalApiException;
import dev.vernite.vernite.integration.git.Branch;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.Repository;
import dev.vernite.vernite.integration.git.github.api.GitHubApiClient;
import dev.vernite.vernite.integration.git.github.api.GitHubConfiguration;
import dev.vernite.vernite.integration.git.github.api.model.GitHubIssue;
import dev.vernite.vernite.integration.git.github.api.model.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.api.model.GitHubRelease;
import dev.vernite.vernite.integration.git.github.api.model.GitHubRepository;
import dev.vernite.vernite.integration.git.github.api.model.Installations;
import dev.vernite.vernite.integration.git.github.api.model.Repositories;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthRefreshTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthTokenRequest;
import dev.vernite.vernite.integration.git.github.data.BranchName;
import dev.vernite.vernite.integration.git.github.model.Authorization;
import dev.vernite.vernite.integration.git.github.model.AuthorizationRepository;
import dev.vernite.vernite.integration.git.github.model.Installation;
import dev.vernite.vernite.integration.git.github.model.InstallationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.integration.git.github.model.TaskIntegration;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationId;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for GitHub integration.
 */
@Service
public class GitHubService {

    private final GitHubApiClient client;

    private GitHubConfiguration config;

    private AuthorizationRepository authorizationRepository;

    private InstallationRepository installationRepository;

    private ProjectIntegrationRepository projectIntegrationRepository;

    private TaskIntegrationRepository taskIntegrationRepository;

    public GitHubService(GitHubConfiguration config, AuthorizationRepository authorizationRepository,
            InstallationRepository installationRepository, ProjectIntegrationRepository projectIntegrationRepository,
            TaskIntegrationRepository taskIntegrationRepository) {
        this.config = config;
        this.authorizationRepository = authorizationRepository;
        this.installationRepository = installationRepository;
        this.projectIntegrationRepository = projectIntegrationRepository;
        this.taskIntegrationRepository = taskIntegrationRepository;

        var webClient = WebClient.builder().baseUrl(config.getApiURL())
                .defaultStatusHandler(HttpStatusCode::isError,
                        resp -> Mono.error(new ExternalApiException("github", "github error" + resp.statusCode())))
                .build();
        var adapter = WebClientAdapter.forClient(webClient);
        client = HttpServiceProxyFactory.builder(adapter).build().createClient(GitHubApiClient.class);
    }

    /**
     * Get the GitHub OAuth installation URL for the given state.
     * 
     * @param state the state to pass to GitHub
     * @return the installation URL
     * @throws URISyntaxException if the URL cannot be built
     */
    public URI getAuthorizationUrl(String state) throws URISyntaxException {
        var builder = new URIBuilder(GitHubConfiguration.GITHUB_AUTH_URL);
        builder.addParameter("client_id", config.getClientId());
        builder.addParameter("state", state);
        return builder.build();
    }

    /**
     * Create an authorization for the given user and code.
     * 
     * @param user the user to create the authorization for
     * @param code the code to use to create the authorization
     * @return the authorization
     */
    public Mono<Authorization> createAuthorization(User user, String code) {
        var request = new OauthTokenRequest(config.getClientId(), config.getClientSecret(), code);
        return client.createOauthAccessToken(request)
                .flatMap(token -> client.getAuthenticatedUser("Bearer " + token.getAccessToken()).map(githubUser -> {
                    var auth = authorizationRepository.findById(githubUser.getId()).orElseGet(Authorization::new);
                    auth.update(token, githubUser, user);
                    return authorizationRepository.save(auth);
                }));
    }

    /**
     * Retrieves repositories available for user from GitHub api.
     * 
     * @param user the user
     * @return list with all repositories
     */
    public Flux<Repository> getUserRepositories(User user) {
        return Flux.fromIterable(authorizationRepository.findByUser(user))
                .flatMap(this::refreshToken)
                .flatMap(this::getUserInstallations)
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMap(client::getInstallationRepositories)
                .flatMapIterable(Repositories::getRepositoryList)
                .map(repo -> new Repository(repo.getId(), repo.getName(), repo.getFullName(), repo.getHtmlUrl(),
                        repo.isPrivate(), "github"));
    }

    /**
     * Create a project integration for the given project and repository.
     * 
     * @param user               the user; must have an authorization to the
     *                           repository
     * @param project            the project
     * @param repositoryFullName the repository full name
     * @return the project integration
     */
    public Mono<ProjectIntegration> createProjectIntegration(User user, Project project, String repositoryFullName) {
        return Flux.fromIterable(authorizationRepository.findByUser(user))
                .flatMap(this::refreshToken)
                .flatMap(this::getUserInstallations)
                .flatMap(this::refreshToken)
                .filterWhen(inst -> hasRepository(inst, repositoryFullName))
                .reduce(Optional.<Installation>empty(), (acc, inst) -> Optional.of(inst))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(inst -> new ProjectIntegration(repositoryFullName, project, inst))
                .map(projectIntegrationRepository::save);
    }

    /**
     * Get issues for the given project.
     * 
     * @param project the project
     * @return the issues
     */
    public Flux<Issue> getIssues(Project project) {
        var integrationOptional = projectIntegrationRepository.findByProject(project);

        if (integrationOptional.isEmpty()) {
            return Flux.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMapMany(token -> client.getRepositoryIssues(token, owner, repo))
                .map(GitHubIssue::toIssue);
    }

    /**
     * Connect the given task to the given issue.
     * 
     * @param task the task
     * @param id   the issue id
     * @return the issue
     */
    public Mono<Issue> connectIssue(Task task, long id) {
        var integrationOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        Set<Long> assignees = new HashSet<>();
        if (task.getAssignee() != null) {
            authorizationRepository.findByUser(task.getAssignee()).forEach(auth -> assignees.add(auth.getId()));
        }

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMap(token -> client.getRepositoryIssue(token, owner, repo, id))
                .map(GitHubIssue::toIssue)
                .map(issue -> new TaskIntegration(task, integration, id, TaskIntegration.Type.ISSUE))
                .map(taskIntegrationRepository::save)
                .then(patchIssue(task));
    }

    /**
     * Create a GitHub issue for the given task.
     * 
     * @param task the task
     * @return the issue
     */
    public Mono<Issue> createIssue(Task task) {
        var integrationOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        var issue = new GitHubIssue(0, null, null, task.getName(), task.getDescription(), new ArrayList<>());
        Set<Long> assignees = new HashSet<>();
        if (task.getAssignee() != null) {
            authorizationRepository.findByUser(task.getAssignee()).forEach(auth -> assignees.add(auth.getId()));
        }

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .flatMap(inst -> setCollaborators(inst, owner, repo, issue, assignees))
                .flatMap(inst -> client.createRepositoryIssue("Bearer " + inst.getToken(), owner, repo, issue))
                .map(newIssue -> {
                    var i = new TaskIntegration(task, integration, newIssue.getNumber(), TaskIntegration.Type.ISSUE);
                    taskIntegrationRepository.save(i);
                    return newIssue.toIssue();
                });
    }

    /**
     * Update the GitHub issue for the given task.
     * 
     * @param task the task
     * @return the issue
     */
    public Mono<Issue> patchIssue(Task task) {
        var integrationProjectOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationProjectOptional.isEmpty()) {
            return Mono.empty();
        }

        var integrationProject = integrationProjectOptional.get();

        var integrationOptional = taskIntegrationRepository.findById(
                new TaskIntegrationId(task.getId(), integrationProject.getId(), TaskIntegration.Type.ISSUE.ordinal()));

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();

        var issue = new GitHubIssue(integration.getIssueId(), integration.link(),
                task.getStatus().isFinal() ? "closed" : "open", task.getName(), task.getDescription(),
                new ArrayList<>());
        Set<Long> assignees = new HashSet<>();
        if (task.getAssignee() != null) {
            authorizationRepository.findByUser(task.getAssignee()).forEach(auth -> assignees.add(auth.getId()));
        }

        var owner = integrationProject.getRepositoryOwner();
        var repo = integrationProject.getRepositoryName();

        return Mono.just(integrationProject.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .flatMap(inst -> setCollaborators(inst, owner, repo, issue, assignees))
                .flatMap(inst -> client.patchRepositoryIssue("Bearer " + inst.getToken(), owner, repo,
                        issue.getNumber(), issue))
                .map(gitIssue -> gitIssue.toIssue());
    }

    /**
     * Delete the GitHub issue connection for the given task.
     * 
     * @param task the task
     */
    public void deleteIssue(Task task) {
        var integrationOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationOptional.isEmpty()) {
            return;
        }

        var integration = integrationOptional.get();

        taskIntegrationRepository
                .findById(
                        new TaskIntegrationId(task.getId(), integration.getId(), TaskIntegration.Type.ISSUE.ordinal()))
                .ifPresent(taskIntegrationRepository::delete);
    }

    /**
     * Get the GitHub pull requests for the given project.
     * 
     * @param project the project
     * @return the pull requests
     */
    public Flux<PullRequest> getPullRequests(Project project) {
        var integrationOptional = projectIntegrationRepository.findByProject(project);

        if (integrationOptional.isEmpty()) {
            return Flux.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMapMany(token -> client.getRepositoryPullRequests(token, owner, repo))
                .map(GitHubPullRequest::toPullRequest);
    }

    /**
     * Connect the given task to the given GitHub pull request.
     * 
     * @param task the task
     * @param id   the pull request id
     * @return the pull request
     */
    public Mono<PullRequest> connectPullRequest(Task task, long id) {
        var integrationOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        Set<Long> assignees = new HashSet<>();
        if (task.getAssignee() != null) {
            authorizationRepository.findByUser(task.getAssignee()).forEach(auth -> assignees.add(auth.getId()));
        }

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMap(token -> client.getRepositoryPullRequest(token, owner, repo, id))
                .map(GitHubPullRequest::toPullRequest)
                .map(pull -> {
                    var i = new TaskIntegration(task, integration, id, TaskIntegration.Type.PULL_REQUEST);
                    taskIntegrationRepository.save(i);
                    return pull;
                });
    }

    /**
     * Patch the GitHub pull request for the given task.
     * 
     * @param task the task
     * @return the pull request
     */
    public Mono<PullRequest> patchPullRequest(Task task) {
        var integrationProjectOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationProjectOptional.isEmpty()) {
            return Mono.empty();
        }

        var integrationProject = integrationProjectOptional.get();

        var integrationOptional = taskIntegrationRepository.findById(new TaskIntegrationId(task.getId(),
                integrationProject.getId(), TaskIntegration.Type.PULL_REQUEST.ordinal()));

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();

        var pullRequest = new GitHubPullRequest(integration.getIssueId(), null, null, task.getName(),
                task.getDescription(), new ArrayList<>(), null, false);

        Set<Long> assignees = new HashSet<>();
        if (task.getAssignee() != null) {
            authorizationRepository.findByUser(task.getAssignee()).forEach(auth -> assignees.add(auth.getId()));
        }

        var owner = integrationProject.getRepositoryOwner();
        var repo = integrationProject.getRepositoryName();

        if (task.getStatus().isFinal()) {
            return refreshToken(integrationProject.getInstallation())
                    .filter(inst -> !inst.isSuspended())
                    .flatMap(this::refreshToken)
                    .flatMap(inst -> client.mergePullRequest("Bearer " + inst.getToken(), owner, repo,
                            integration.getIssueId()))
                    .map(merge -> {
                        integration.setMerged(merge.isMerged());
                        return integration;
                    })
                    .map(taskIntegrationRepository::save)
                    .then(Mono.empty());
        }

        return Mono.just(integrationProject.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .flatMap(inst -> setCollaborators(inst, owner, repo, (GitHubIssue) pullRequest, assignees))
                .flatMap(inst -> client.patchRepositoryPullRequest("Bearer " + inst.getToken(), owner, repo,
                        pullRequest.getNumber(), pullRequest))
                .map(pull -> pull.toPullRequest());
    }

    /**
     * Delete the GitHub pull request for the given task.
     * 
     * @param task the task
     */
    public void deletePullRequest(Task task) {
        var integrationOptional = projectIntegrationRepository.findByProject(task.getStatus().getProject());

        if (integrationOptional.isEmpty()) {
            return;
        }

        var integration = integrationOptional.get();

        taskIntegrationRepository
                .findById(new TaskIntegrationId(task.getId(), integration.getId(),
                        TaskIntegration.Type.PULL_REQUEST.ordinal()))
                .ifPresent(taskIntegrationRepository::delete);
    }

    /**
     * Get git branches for the given project.
     * 
     * @param project the project
     * @return the branches
     */
    public Flux<Branch> getBranches(Project project) {
        var integrationOptional = projectIntegrationRepository.findByProject(project);

        if (integrationOptional.isEmpty()) {
            return Flux.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMapMany(token -> client.getRepositoryBranches(token, owner, repo))
                .map(BranchName::toBranch);
    }

    /**
     * Create new release for the given project.
     * 
     * @param release the release
     * @param branch  the branch
     * @return the release
     */
    public Mono<Long> publishRelease(Release release, String branch) {
        var integrationOptional = projectIntegrationRepository.findByProject(release.getProject());

        if (integrationOptional.isEmpty()) {
            return Mono.empty();
        }

        var integration = integrationOptional.get();
        var owner = integration.getRepositoryOwner();
        var repo = integration.getRepositoryName();

        return Mono.just(integration.getInstallation())
                .filter(inst -> !inst.isSuspended())
                .flatMap(this::refreshToken)
                .map(Installation::getToken)
                .map(token -> "Bearer " + token)
                .flatMap(token -> client.createRepositoryRelease(token, owner, repo, new GitHubRelease(release)))
                .map(GitHubRelease::getId);
    }

    private Mono<Installation> refreshToken(Installation installation) {
        return installation.shouldRefreshToken()
                ? client.createInstallationAccessToken("Bearer " + createJWT(), installation.getId()).map(token -> {
                    installation.refreshToken(token);
                    return installationRepository.save(installation);
                })
                : Mono.just(installation);
    }

    private Mono<Authorization> refreshToken(Authorization authorization) {
        if (authorization.shouldRefreshToken()) {
            var request = new OauthRefreshTokenRequest(authorization.getRefreshToken(), "refresh_token",
                    config.getClientId(), config.getClientSecret());
            return client.refreshOauthAccessToken(request).map(token -> {
                authorization.refreshToken(token);
                return authorizationRepository.save(authorization);
            });
        }
        return Mono.just(authorization);
    }

    private Flux<Installation> getUserInstallations(Authorization authorization) {
        return client.getUserInstallations("Bearer " + authorization.getAccessToken())
                .map(Installations::getInstallationList)
                .flatMapMany(Flux::fromIterable)
                .filter(installation -> installation.getAppId() == config.getAppId())
                .map(installation -> {
                    var inst = installationRepository.findById(installation.getId()).orElseGet(Installation::new);
                    inst.update(installation);
                    return installationRepository.save(inst);
                });
    }

    private Mono<Boolean> hasRepository(Installation installation, String repositoryFullName) {
        return client.getInstallationRepositories("Bearer " + installation.getToken())
                .map(Repositories::getRepositoryList)
                .flatMapMany(Flux::fromIterable)
                .map(GitHubRepository::getFullName)
                .any(repositoryFullName::equals);
    }

    private Mono<Installation> setCollaborators(Installation installation, String owner, String name,
            GitHubIssue issue, Set<Long> assignees) {
        if (assignees.isEmpty()) {
            return Mono.just(installation);
        }
        return client.getRepositoryCollaborators("Bearer " + installation.getToken(), owner, name)
                .filter(user -> assignees.contains(user.getId()))
                .map(user -> issue.getAssignees().add(user.getLogin()))
                .then(Mono.just(installation));
    }

    private String createJWT() {
        var now = Instant.now();
        return Jwts.builder().setIssuedAt(Date.from(now)).setIssuer(Long.toString(config.getAppId()))
                .signWith(config.getJwtKey()).setExpiration(Date.from(now.plusSeconds(60))).compact();
    }

}
