package com.workflow.workflow.integration.git.github;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationRepositories;
import com.workflow.workflow.integration.git.github.data.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.data.InstallationToken;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.integration.git.github.entity.GitHubTaskRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Component
public class GitHubService {
    private static final String APP_ID = "195507";
    private static final String AUTHORIZATION = "Authorization";
    private static final String INTEGRATION_LINK = "https://github.com/apps/workflow-2022/installations/new";
    private static final String ACCEPT = "Accept";
    private static final String BEARER = "Bearer ";
    private static final String APPLICATION_JSON_GITHUB = "application/vnd.github.v3+json";
    private WebClient client = WebClient.create("https://api.github.com");
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubTaskRepository taskRepository;

    /**
     * Retrieves repositories available for user from GitHub api.
     * 
     * @param user must not be {@literal null}; must be entity from database.
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
     * Cretaes new installation for user.
     * 
     * @param user must not be {@literal null}; must be entity from database.
     * @param id   must not be {@literal null}; must be id of installation retrieved
     *             from GitHub.
     * @return Mono with list of repositories and link. Can be empty.
     */
    public Mono<GitHubIntegrationInfo> newInstallation(User user, long id) {
        return apiGetInstallation(id)
                .map(GitHubInstallationApi::getAccount)
                .map(gitUser -> installationRepository.save(new GitHubInstallation(id, user, gitUser.getLogin())))
                .flatMap(this::refreshToken)
                .flatMap(this::apiGetInstallationRepositories)
                .map(GitHubInstallationRepositories::getRepositories)
                .map(repositories -> new GitHubIntegrationInfo(INTEGRATION_LINK, repositories));
    }

    /**
     * Creates new integration for project.
     * 
     * @param user     must not be {@literal null}; must be entity from database.
     * @param project  must not be {@literal null}; must be entity from database.
     * @param fullName must not be {@literal null}; must be valid GitHub repository
     *                 name.
     * @return Mono with created integration. Can be empty when repository is not
     *         found.
     */
    public Mono<GitHubIntegration> newIntegration(User user, Project project, String fullName) {
        return Flux.fromIterable(installationRepository.findByUserAndSuspendedFalse(user))
                .flatMap(this::refreshToken)
                .filterWhen(installation -> hasRepository(installation, fullName))
                .reduce(Optional.<GitHubInstallation>empty(), (first, second) -> Optional.of(second))
                .filter(Optional::isPresent)
                .map(installation -> integrationRepository
                        .save(new GitHubIntegration(project, installation.get(), fullName)));
    }

    /**
     * Creates issue associeted with task.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return - Mono with issue. Can be empty when GitHub api return error.
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
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> apiPostRepositoryIssue(installation, integration, new GitHubIssue(task)))
                .map(issue -> {
                    taskRepository.save(new GitHubTask(task, integration, issue.getNumber(), (byte) 0));
                    return issue.toIssue();
                });
    }

    /**
     * Modifies associeted issue using GitHub api.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return Mono with issue. Can be empty when GitHub api return error.
     */
    public Mono<Issue> patchIssue(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTask gitHubTask = optional.get();
        if (gitHubTask.getGitHubIntegration().getInstallation().getSuspended()) {
            return Mono.empty();
        }
        GitHubIssue gitHubIssue = new GitHubIssue(task);
        gitHubIssue.setNumber(gitHubTask.getIssueId());
        return refreshToken(gitHubTask.getGitHubIntegration().getInstallation())
                .flatMap(installation -> apiPatchRepositoryIssue(installation, gitHubTask.getGitHubIntegration(),
                        gitHubIssue))
                .map(GitHubIssue::toIssue);
    }

    /**
     * Retrieves issues for project from GitHub api.
     * 
     * @param project must not be {@literal null}; must be entity from database.
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
     * @param task  must not be {@literal null}; must be entity from database.
     * @param issue must not be {@literal null}; must be issue from GitHub api.
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
                    taskRepository.save(new GitHubTask(task, optional.get(), gitHubIssue.getNumber(), (byte) 0));
                    return gitHubIssue.toIssue();
                });
    }

    /**
     * Deletes task connection to issue.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     */
    public void deleteIssue(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0);
        if (optional.isEmpty()) {
            return;
        }
        GitHubTask gitHubTask = optional.get();
        gitHubTask.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        taskRepository.save(gitHubTask);
    }

    /**
     * Retrieves pull requests for project from GitHub api.
     * 
     * @param project must not be {@literal null}; must be entity from database.
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
     * @param task        must not be {@literal null}; must be entity from database.
     * @param pullRequest must not be {@literal null}; must be pull request from
     *                    GitHub api.
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
                    taskRepository.save(new GitHubTask(task, integration, gitHubPullRequest.getNumber(), (byte) 1));
                    return gitHubPullRequest.toPullRequest();
                });
    }

    /**
     * Modifies associeted pull request using GitHub api.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return Mono with pull request. Can be empty when GitHub api return error.
     */
    public Mono<Issue> patchPullRequest(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 1);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTask gitHubTask = optional.get();
        GitHubIntegration integration = gitHubTask.getGitHubIntegration();
        if (integration.getInstallation().getSuspended()) {
            return Mono.empty();
        }
        GitHubPullRequest gitHubPullRequest = new GitHubPullRequest();
        if (Boolean.TRUE.equals(task.getStatus().isFinal())) {
            return refreshToken(integration.getInstallation())
                    .flatMap(installation -> apiPutRepositoryPull(installation, integration, gitHubTask.getIssueId()))
                    .map(v -> {
                        gitHubTask.setIsPullRequest((byte) 2);
                        taskRepository.save(gitHubTask);
                        return v;
                    })
                    .then(Mono.empty());
        }
        gitHubPullRequest.setState("open");
        gitHubPullRequest.setNumber(gitHubTask.getIssueId());
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> apiPatchRepositoryPull(installation, integration, gitHubPullRequest))
                .map(GitHubPullRequest::toPullRequest);
    }

    /**
     * Deletes task connection to pull request.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     */
    public void deletePullRequest(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 1);
        if (optional.isPresent()) {
            GitHubTask gitHubTask = optional.get();
            gitHubTask.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
            taskRepository.save(gitHubTask);
        }
    }

    /**
     * Checks if repository with given name belongs to installation.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database. Must not be suspended. Token must be valid.
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
     * Checks if token needs to be refreshed and refreshes it when
     * needed. When token does not need refreshing it does nothing.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database.
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
     * Gets information abount installation from GitHub api.
     * 
     * @param id must be given from GitHub.
     * @return Mono with installation information or {@literial null} if GitHub api
     *         gives error.
     */
    private Mono<GitHubInstallationApi> apiGetInstallation(long id) {
        return client.get()
                .uri("/app/installations/{id}", id)
                .header(AUTHORIZATION, BEARER + createJWT())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubInstallationApi.class)
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Request GitHub api for new token.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @return Mono with new token or {@literal null} if GitHub api gives error.
     */
    private Mono<InstallationToken> apiPostInstallationToken(GitHubInstallation installation) {
        return client.post()
                .uri("/app/installations/{id}/access_tokens", installation.getInstallationId())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .header(AUTHORIZATION, BEARER + createJWT())
                .retrieve()
                .bodyToMono(InstallationToken.class)
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Retrives repositories for installation from GitHub api.
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
                .onErrorReturn(new GitHubInstallationRepositories());
    }

    /**
     * Retrives issue for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @return Flux with issues. Can be empty.
     */
    private Flux<GitHubIssue> apiGetRepositoryIssues(GitHubInstallation installation, GitHubIntegration integration) {
        return client.get()
                .uri("/repo/{owner}/{repo}/issues", integration.getRepositoryOwner(), integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubIssue.class)
                .onErrorResume(error -> Flux.empty());
    }

    /**
     * Creates new issue for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue or {@literal null} if GitHub api gives error.
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
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Modifies issue for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue or {@literal null} if GitHub api gives error.
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
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Retrieves issue for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param issue        must not be {@literal null}.
     * @return Mono with issue or {@literal null} if GitHub api gives error.
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
                .onErrorResume(error -> Mono.empty());
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
                .uri("/repo/{owner}/{repo}/pulls", integration.getRepositoryOwner(), integration.getRepositoryName())
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToFlux(GitHubPullRequest.class)
                .onErrorResume(error -> Flux.empty());
    }

    /**
     * Modifies pull request for repository using GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param pull         must not be {@literal null}.
     * @return Mono with pull request or {@literal null} if GitHub api gives error.
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
                .onErrorResume(error -> Mono.empty());
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
    private Mono<Void> apiPutRepositoryPull(GitHubInstallation installation, GitHubIntegration integration, long pull) {
        return client.put()
                .uri("/repos/{owner}/{repo}/pulls/{id}/merge", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), pull)
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(Void.class)
                .onErrorResume(error -> Mono.empty());
    }

    /**
     * Retrieves pull request for repository from GitHub api.
     * 
     * @param installation must not be {@literal null}. Must be entity from
     *                     database.
     * @param integration  must not be {@literal null}. Must be entity from
     *                     database.
     * @param pull         must not be {@literal null}.
     * @return Mono with pull request or {@literal null} if GitHub api gives error.
     */
    private Mono<GitHubPullRequest> apiGetRepositoryPull(GitHubInstallation installation,
            GitHubIntegration integration, long pull) {
        return client.get()
                .uri("/repo/{owner}/{repo}/pulls/{id}", integration.getRepositoryOwner(),
                        integration.getRepositoryName(), pull)
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubPullRequest.class)
                .onErrorResume(error -> Mono.empty());
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
                    Files.readAllBytes(Path.of("workflow-2022.private-key.der")));
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
}
