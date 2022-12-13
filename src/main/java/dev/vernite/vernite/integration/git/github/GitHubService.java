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

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import dev.vernite.vernite.integration.git.Branch;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubBranchRead;
import dev.vernite.vernite.integration.git.github.data.GitHubInstallationApi;
import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.data.GitHubMergeInfo;
import dev.vernite.vernite.integration.git.github.data.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubRelease;
import dev.vernite.vernite.integration.git.github.data.GitHubRepository;
import dev.vernite.vernite.integration.git.github.data.GitHubUser;
import dev.vernite.vernite.integration.git.github.data.GitHubInstallationRepositories;
import dev.vernite.vernite.integration.git.github.data.GitHubIntegrationInfo;
import dev.vernite.vernite.integration.git.github.data.InstallationToken;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallation;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallationRepository;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegrationRepository;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskIssue;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskIssueRepository;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskPull;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskPullRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ExternalApiException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Deprecated
@Service
@Component
public class GitHubService {
    private static final String APP_ID = "195507";
    private static final String AUTHORIZATION = "Authorization";
    public static final String INTEGRATION_LINK = "https://github.com/apps/vernite/installations/new";
    private static final String ACCEPT = "Accept";
    private static final String BEARER = "Bearer ";
    private static final String APPLICATION_JSON_GITHUB = "application/vnd.github.v3+json";
    private static final String GITHUB = "github";

    private WebClient client = WebClient.create("https://api.github.com");
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubTaskIssueRepository issueRepository;
    @Autowired
    private GitHubTaskPullRepository pullRepository;

    /**
     * Retrieves repositories available for user from GitHub api.
     * 
     * @param user must be entity from database.
     * @return Mono with list of repositories and link.
     */
    public Mono<GitHubIntegrationInfo> getRepositories(User user) {
        return Flux.fromIterable(installationRepository.findByUserAndSuspendedFalse(user))
                .flatMap(this::refreshToken)
                .flatMap(this::apiGetInstallationRepositories)
                .map(GitHubInstallationRepositories::getRepositories)
                .reduce(new ArrayList<GitHubRepository>(), (first, second) -> {
                    first.addAll(second);
                    return first;
                })
                .map(repositories -> new GitHubIntegrationInfo(INTEGRATION_LINK, repositories))
                .defaultIfEmpty(new GitHubIntegrationInfo(INTEGRATION_LINK, List.of()));
    }

    /**
     * Creates new installation for user.
     * 
     * @param user must be entity from database.
     * @param id   must be id of installation retrieved from GitHub.
     * @return Mono with list of repositories and link. Can be empty.
     */
    public Mono<GitHubIntegrationInfo> newInstallation(User user, long id) {
        return apiGetInstallation(id)
                .map(installation -> installationRepository.save(new GitHubInstallation(id, user, installation)))
                .flatMap(this::refreshToken)
                .flatMap(this::apiGetInstallationRepositories)
                .map(GitHubInstallationRepositories::getRepositories)
                .map(repositories -> new GitHubIntegrationInfo(INTEGRATION_LINK, repositories));
    }

    /**
     * Creates new integration for project.
     * 
     * @param user     must be entity from database.
     * @param project  must be entity from database.
     * @param fullName must be valid GitHub repository name.
     * @return Mono with created integration. Can be empty when repository is not
     *         found.
     */
    public Mono<GitHubIntegration> newIntegration(User user, Project project, String fullName) {
        return Flux.fromIterable(installationRepository.findByUserAndSuspendedFalse(user))
                .flatMap(this::refreshToken)
                .filterWhen(installation -> hasRepository(installation, fullName))
                .reduce(Optional.<GitHubInstallation>empty(), (first, second) -> Optional.of(second))
                .filter(Optional::isPresent)
                .map(inst -> integrationRepository.save(new GitHubIntegration(project, inst.get(), fullName)));
    }

    /**
     * Creates issue associated with task.
     * 
     * @param task must be entity from database.
     * @return - Mono with issue.
     */
    public Mono<Issue> createIssue(Task task) {
        Optional<GitHubIntegration> optional = integrationRepository
                .findByProjectAndActiveNull(task.getStatus().getProject());
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubIntegration integration = optional.get();
        if (integration.getInstallation().getSuspended()) {
            return Mono.empty();
        }
        List<GitHubInstallation> installations = new ArrayList<>();
        if (task.getAssignee() != null) {
            installations.addAll(installationRepository.findByUser(task.getAssignee()));
        }
        GitHubIssue issue = new GitHubIssue(task, List.of());
        return refreshToken(integration.getInstallation())
                .flatMap(inst -> hasCollaborator(inst, integration, issue, installations))
                .flatMap(installation -> apiPostRepositoryIssue(installation, integration, issue))
                .map(i -> {
                    issueRepository.save(new GitHubTaskIssue(task, integration, i));
                    return i.toIssue();
                });
    }

    /**
     * Modifies associated issue using GitHub api.
     * 
     * @param task must be entity from database.
     * @return Mono with issue.
     */
    public Mono<Issue> patchIssue(Task task) {
        Optional<GitHubTaskIssue> optional = issueRepository.findByTask(task);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTaskIssue gitHubTask = optional.get();
        if (gitHubTask.getGitHubIntegration().getInstallation().getSuspended()) {
            return Mono.empty();
        }
        List<GitHubInstallation> installations = new ArrayList<>();
        if (task.getAssignee() != null) {
            installations.addAll(installationRepository.findByUser(task.getAssignee()));
        }
        GitHubIssue gitHubIssue = new GitHubIssue(task, List.of());
        gitHubIssue.setNumber(gitHubTask.getIssueId());
        return refreshToken(gitHubTask.getGitHubIntegration().getInstallation())
                .flatMap(inst -> hasCollaborator(inst, gitHubTask.getGitHubIntegration(), gitHubIssue, installations))
                .flatMap(inst -> apiPatchRepositoryIssue(inst, gitHubTask.getGitHubIntegration(), gitHubIssue))
                .map(GitHubIssue::toIssue);
    }

    /**
     * Retrieves issues for project from GitHub api.
     * 
     * @param project must be entity from database.
     * @return Flux with issues. Can be empty.
     */
    public Flux<Issue> getIssues(Project project) {
        Optional<GitHubIntegration> optional = integrationRepository.findByProjectAndActiveNull(project);
        if (optional.isEmpty()) {
            return Flux.empty();
        }
        GitHubIntegration integration = optional.get();
        if (integration.getInstallation().getSuspended()) {
            return Flux.empty();
        }
        return refreshToken(integration.getInstallation())
                .flatMapMany(installation -> apiGetRepositoryIssues(installation, integration))
                .map(GitHubIssue::toIssue);
    }

    /**
     * Connects task to issue.
     * 
     * @param task  must be entity from database.
     * @param issue must be issue from GitHub api.
     * @return Mono with issue. Can be empty.
     */
    public Mono<Issue> connectIssue(Task task, Issue issue) {
        Optional<GitHubIntegration> optional = integrationRepository
                .findByProjectAndActiveNull(task.getStatus().getProject());
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubIntegration integration = optional.get();
        if (integration.getInstallation().getSuspended()) {
            return Mono.empty();
        }
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> apiGetRepositoryIssue(installation, integration, issue.getId()))
                .map(gitHubIssue -> {
                    issueRepository.save(new GitHubTaskIssue(task, integration, gitHubIssue));
                    return gitHubIssue.toIssue();
                });
    }

    /**
     * Deletes task connection to issue.
     * 
     * @param task must be entity from database.
     */
    public void deleteIssue(Task task) {
        issueRepository.findByTask(task).ifPresent(issueRepository::delete);
    }

    /**
     * Retrieves pull requests for project from GitHub api.
     * 
     * @param project must be entity from database.
     * @return Flux with pull requests. Can be empty.
     */
    public Flux<PullRequest> getPullRequests(Project project) {
        Optional<GitHubIntegration> optional = integrationRepository.findByProjectAndActiveNull(project);
        if (optional.isEmpty()) {
            return Flux.empty();
        }
        GitHubIntegration integration = optional.get();
        if (integration.getInstallation().getSuspended()) {
            return Flux.empty();
        }
        return refreshToken(integration.getInstallation())
                .flatMapMany(installation -> apiGetRepositoryPulls(installation, integration))
                .map(GitHubPullRequest::toPullRequest);
    }

    /**
     * Connects task to pull request.
     * 
     * @param task        must be entity from database.
     * @param pullRequest must be pull request from GitHub api.
     * @return Mono with pull request. Can be empty.
     */
    public Mono<PullRequest> connectPullRequest(Task task, PullRequest pullRequest) {
        Optional<GitHubIntegration> optional = integrationRepository
                .findByProjectAndActiveNull(task.getStatus().getProject());
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubIntegration integration = optional.get();
        if (integration.getInstallation().getSuspended()) {
            return Mono.empty();
        }
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> apiGetRepositoryPull(installation, integration, pullRequest.getId()))
                .map(gitHubPullRequest -> {
                    pullRepository.save(new GitHubTaskPull(task, integration, gitHubPullRequest));
                    return gitHubPullRequest.toPullRequest();
                });
    }

    /**
     * Modifies associated pull request using GitHub api.
     * 
     * @param task must be entity from database.
     * @return Mono with pull request.
     */
    public Mono<Issue> patchPullRequest(Task task) {
        Optional<GitHubTaskPull> optional = pullRepository.findByTask(task);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTaskPull gitHubTask = optional.get();
        GitHubIntegration integration = gitHubTask.getGitHubIntegration();
        if (integration.getInstallation().getSuspended()) {
            return Mono.empty();
        }
        GitHubPullRequest gitHubPullRequest = new GitHubPullRequest();
        if (Boolean.TRUE.equals(task.getStatus().isFinal())) {
            return refreshToken(integration.getInstallation())
                    .flatMap(installation -> apiPutRepositoryPull(installation, integration, gitHubTask.getIssueId()))
                    .map(mergeInfo -> {
                        if (mergeInfo.isMerged()) {
                            gitHubTask.setMerged(true);
                            pullRepository.save(gitHubTask);
                        }
                        return new Issue(-1, gitHubTask.getLink(), mergeInfo.getMessage(), mergeInfo.getSha(),
                                GITHUB);
                    })
                    .then(Mono.empty());
        }
        List<GitHubInstallation> installations = new ArrayList<>();
        if (task.getAssignee() != null) {
            installations.addAll(installationRepository.findByUser(task.getAssignee()));
        }
        if (!gitHubTask.getMerged()) {
            gitHubPullRequest.setState("open");
        }
        gitHubPullRequest.setNumber(gitHubTask.getIssueId());
        return refreshToken(integration.getInstallation())
                .flatMap(inst -> hasCollaborator(inst, gitHubTask.getGitHubIntegration(), gitHubPullRequest,
                        installations))
                .flatMap(installation -> apiPatchRepositoryPull(installation, integration, gitHubPullRequest))
                .map(GitHubPullRequest::toPullRequest);
    }

    /**
     * Deletes task connection to pull request.
     * 
     * @param task must be entity from database.
     */
    public void deletePullRequest(Task task) {
        pullRepository.findByTask(task).ifPresent(pullRepository::delete);
    }

    /**
     * Checks if repository with given name belongs to installation.
     * 
     * @param installation must be entity from database. Must not be suspended.
     *                     Token must be valid.
     * @param fullName     full name of repository which is looked for.
     * @return Mono with boolean.
     */
    private Mono<Boolean> hasRepository(GitHubInstallation installation, String fullName) {
        return apiGetInstallationRepositories(installation)
                .map(GitHubInstallationRepositories::getRepositories)
                .map(repositories -> repositories.stream()
                        .anyMatch(repository -> repository.getFullName().equals(fullName)));
    }

    /**
     * Removes assigned user from issue if is not collaborator.
     * 
     * @param installation must be entity from database. Must not be suspended.
     * @param integration  must be entity from database.
     * @param issue        must be issue from GitHub api.
     * @return Mono with installation.
     */
    private Mono<GitHubInstallation> hasCollaborator(GitHubInstallation installation, GitHubIntegration integration,
            GitHubIssue issue, List<GitHubInstallation> installations) {
        if (installations.isEmpty()) {
            return Mono.just(installation);
        }
        return apiGetRepositoryCollaborators(installation, integration)
                .any(collaborator -> {
                    for (GitHubInstallation inst : installations) {
                        if (collaborator.getLogin().equals(inst.getGitHubUsername())) {
                            issue.setAssignees(List.of(inst.getGitHubUsername()));
                            return true;
                        }
                    }
                    return false;
                })
                .thenReturn(installation);
    }

    /**
     * Checks if token needs to be refreshed and refreshes it when
     * needed. When token does not need refreshing it does nothing.
     * 
     * @param installation must be entity from database.
     * @return Mono with installation.
     */
    private Mono<GitHubInstallation> refreshToken(GitHubInstallation installation) {
        if (Instant.now().isAfter(installation.getExpiresAt().toInstant())) {
            return apiPostInstallationToken(installation)
                    .map(token -> {
                        installation.updateToken(token);
                        return installationRepository.save(installation);
                    });
        }
        return Mono.just(installation);
    }

    /**
     * Gets information about installation from GitHub api.
     * 
     * @param id must be given from GitHub.
     * @return Mono with installation information.
     */
    private Mono<GitHubInstallationApi> apiGetInstallation(long id) {
        return client.get()
                .uri("/app/installations/{id}", id)
                .header(AUTHORIZATION, BEARER + createJWT())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubInstallationApi.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Request GitHub api for new token.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @return Mono with new token.
     */
    private Mono<InstallationToken> apiPostInstallationToken(GitHubInstallation installation) {
        return client.post()
                .uri("/app/installations/{id}/access_tokens", installation.getInstallationId())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .header(AUTHORIZATION, BEARER + createJWT())
                .retrieve()
                .bodyToMono(InstallationToken.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves repositories for installation from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @return Mono with list of repositories; can be empty.
     */
    private Mono<GitHubInstallationRepositories> apiGetInstallationRepositories(GitHubInstallation installation) {
        return client.get()
                .uri("/installation/repositories")
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubInstallationRepositories.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves issue for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @return Flux with issues. Can be empty.
     */
    private Flux<GitHubIssue> apiGetRepositoryIssues(GitHubInstallation installation, GitHubIntegration integration) {
        return client.get()
                .uri("/repos/{owner}/{repo}/issues", integration.getRepositoryOwner(), integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubIssue.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Creates new issue for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue.
     */
    private Mono<GitHubIssue> apiPostRepositoryIssue(GitHubInstallation installation, GitHubIntegration integration,
            GitHubIssue issue) {
        return client.post()
                .uri("/repos/{owner}/{repo}/issues", integration.getRepositoryOwner(), integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .bodyValue(issue)
                .retrieve()
                .bodyToMono(GitHubIssue.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Modifies issue for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue.
     */
    private Mono<GitHubIssue> apiPatchRepositoryIssue(GitHubInstallation installation, GitHubIntegration integration,
            GitHubIssue issue) {
        return client.patch()
                .uri("/repos/{owner}/{repo}/issues/{id}", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), issue.getNumber())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .bodyValue(issue)
                .retrieve()
                .bodyToMono(GitHubIssue.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves issue for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue.
     */
    private Mono<GitHubIssue> apiGetRepositoryIssue(GitHubInstallation installation, GitHubIntegration integration,
            long issue) {
        return client.get()
                .uri("/repos/{owner}/{repo}/issues/{id}", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), issue)
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubIssue.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves pull requests for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @return Flux with pull requests. Can be empty.
     */
    private Flux<GitHubPullRequest> apiGetRepositoryPulls(GitHubInstallation installation,
            GitHubIntegration integration) {
        return client.get()
                .uri("/repos/{owner}/{repo}/pulls", integration.getRepositoryOwner(), integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubPullRequest.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Modifies pull request for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param pull         must not be {@literal null}.
     * @return Mono with pull request.
     */
    private Mono<GitHubPullRequest> apiPatchRepositoryPull(GitHubInstallation installation,
            GitHubIntegration integration, GitHubPullRequest pull) {
        return client.patch()
                .uri("/repos/{owner}/{repo}/pulls/{id}", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), pull.getNumber())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .bodyValue(pull)
                .retrieve()
                .bodyToMono(GitHubPullRequest.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Merges pull request for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param pull         must not be {@literal null}.
     * @return Mono with nothing.
     */
    private Mono<GitHubMergeInfo> apiPutRepositoryPull(GitHubInstallation installation, GitHubIntegration integration,
            long pull) {
        return client.put()
                .uri("/repos/{owner}/{repo}/pulls/{id}/merge", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), pull)
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubMergeInfo.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves pull request for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param pull         must not be {@literal null}.
     * @return Mono with pull request.
     */
    private Mono<GitHubPullRequest> apiGetRepositoryPull(GitHubInstallation installation,
            GitHubIntegration integration, long pull) {
        return client.get()
                .uri("/repos/{owner}/{repo}/pulls/{id}", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), pull)
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubPullRequest.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Retrieves collaborators for repository from GitHub api.
     * 
     * @param installation must be entity from database.
     * @param integration  must be entity from database.
     * @return Flux with collaborators. Can be empty.
     */
    private Flux<GitHubUser> apiGetRepositoryCollaborators(GitHubInstallation installation,
            GitHubIntegration integration) {
        return client.get()
                .uri("/repos/{owner}/{repo}/collaborators", integration.getRepositoryOwner(),
                        integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubUser.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    private Mono<Long> apiCreateRelease(GitHubInstallation installation,
            GitHubIntegration integration, GitHubRelease release) {
        return client.post()
                .uri("/repos/{owner}/{repo}/releases", integration.getRepositoryOwner(),
                        integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .bodyValue(release)
                .retrieve()
                .bodyToMono(GitHubRelease.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()))
                .map(GitHubRelease::getId);
    }

    private Flux<GitHubBranchRead> apiGetBranches(GitHubInstallation installation,
            GitHubIntegration integration) {
        return client.get()
                .uri("/repos/{owner}/{repo}/branches", integration.getRepositoryOwner(),
                        integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubBranchRead.class)
                .onErrorMap(error -> new ExternalApiException(GITHUB, error.getMessage()));
    }

    /**
     * Creates Json Web Token from file with private key. Token created with this
     * method lasts 10 minutes.
     * 
     * @return Json Web Token.
     */
    private static String createJWT() {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
                    Files.readAllBytes(Path.of("vernite-2022.private-key.der")));
            Key signingKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .setIssuedAt(new Date(now))
                    .setIssuer(APP_ID)
                    .signWith(signingKey)
                    .setExpiration(new Date(now + 600000))
                    .compact();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create JWT");
        }
    }

    public Mono<Long> publishRelease(Release release, String branch) {
        GitHubIntegration integration = release.getProject().getGitHubIntegrationEntity();
        if (integration == null) {
            return Mono.empty();
        }
        GitHubRelease gitHubRelease = new GitHubRelease(release);
        if (branch != null) {
            gitHubRelease.setTargetCommitish(branch);
        }
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> apiCreateRelease(installation, integration, gitHubRelease));
    }

    public Flux<Branch> getBranches(Project project) {
        GitHubIntegration integration = project.getGitHubIntegrationEntity();
        if (integration == null) {
            return Flux.empty();
        }
        return refreshToken(integration.getInstallation())
                .flatMapMany(installation -> apiGetBranches(installation, integration))
                .map(branch -> new Branch(branch.getName()));
    }
}
