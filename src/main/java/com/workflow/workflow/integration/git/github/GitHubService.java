package com.workflow.workflow.integration.git.github;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.integration.git.github.data.GitHubRepositoryList;
import com.workflow.workflow.integration.git.github.data.GitHubUser;
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
import com.workflow.workflow.utils.NotFoundRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;

@Service
public class GitHubService {
    private static final String APP_ID = "195507";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ACCEPT = "Accept";
    private static final String BEARER = "Bearer ";
    private static final String APPLICATION_JSON_GITHUB = "application/vnd.github.v3+json";
    private static final WebClient CLIENT = WebClient.create();
    private GitHubInstallationRepository installationRepository;
    private GitHubIntegrationRepository integrationRepository;
    private GitHubTaskRepository taskRepository;

    public GitHubService(GitHubInstallationRepository installationRepository,
            GitHubIntegrationRepository integrationRepository, GitHubTaskRepository taskRepository) {
        this.installationRepository = installationRepository;
        this.integrationRepository = integrationRepository;
        this.taskRepository = taskRepository;
    }

    /**
     * Retrieves repositories available for user installations from GitHub.
     * 
     * @param user must not be {@literal null}; must be entity from database.
     * @return future containing list with repositories.
     */
    public Mono<List<GitHubRepository>> getRepositories(User user) {
        return Flux.fromIterable(installationRepository.findByUser(user))
                .flatMap(this::getRepositories)
                .reduce(new ArrayList<>(), (first, second) -> {
                    first.addAll(second);
                    return first;
                });
    }

    /**
     * Retrieves repositories available for given installation from GitHub.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database.
     * @return future containing list with repositories.
     */
    public Mono<List<GitHubRepository>> getRepositories(GitHubInstallation installation) {
        return installation.getSuspended() ? Mono.just(List.of())
                : refreshToken(installation).flatMap(this::getRepositoryList);
    }

    /**
     * Retrieves GitHub user information for given installation id.
     * 
     * @param installationId must be id received from GitHub.
     * @return future containing GitHub user.
     */
    public Mono<GitHubUser> getInstallationUser(long installationId) {
        return CLIENT.get()
                .uri("https://api.github.com/app/installations/" + installationId)
                .header(AUTHORIZATION, BEARER + createJWT())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubInstallationApi.class)
                .map(GitHubInstallationApi::getAccount);
    }

    /**
     * Retrieves installation which contains repository with given id for given
     * user.
     * 
     * @param user     must not be {@literal null}; must be entity from
     *                 database.
     * @param fullName full name of repository.
     * @return future containing optional installation.
     */
    public Mono<Optional<GitHubInstallation>> getRepositoryInstallation(User user, String fullName) {
        return Flux.fromIterable(installationRepository.findByUser(user))
                .filterWhen(installation -> this.hasRepository(installation, fullName))
                .reduce(Optional.empty(), (first, second) -> Optional.of(second));
    }

    /**
     * Creates issue associeted with task.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return - future containing created issue.
     */
    public Mono<Issue> createIssue(Task task) {
        Optional<GitHubIntegration> optional = integrationRepository
                .findByProjectAndActiveNull(task.getStatus().getProject());
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubIntegration integration = optional.get();
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> CLIENT.post()
                        .uri(String.format("https://api.github.com/repos/%s/issues",
                                integration.getRepositoryFullName()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .bodyValue(new GitHubIssue(task))
                        .retrieve()
                        .bodyToMono(GitHubIssue.class))
                .map(issue -> {
                    taskRepository.save(new GitHubTask(task, integration, issue.getNumber()));
                    return issue.toIssue();
                });
    }

    /**
     * Modifies issue on GitHub.
     * 
     * @param task must not be {@literal null}; must be entity from database.
     * @return future containing modified issue.
     */
    public Mono<Issue> patchIssue(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNull(task);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTask gitHubTask = optional.get();
        return refreshToken(gitHubTask.getGitHubIntegration().getInstallation())
                .flatMap(installation -> CLIENT.patch()
                        .uri(String.format("https://api.github.com/repos/%s/issues/%d",
                                gitHubTask.getGitHubIntegration().getRepositoryFullName(), gitHubTask.getIssueId()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .bodyValue(new GitHubIssue(task))
                        .retrieve()
                        .bodyToMono(GitHubIssue.class))
                .map(GitHubIssue::toIssue);
    }

    /**
     * Gets issue for integration with given number from GitHub.
     * 
     * @param integration must not be {@literal null}; must be entity from database.
     * @param issueNumber number of searched issue.
     * @return future containing GitHub issue.
     */
    public Mono<GitHubIssue> getIssue(GitHubIntegration integration, long issueNumber) {
        if (integration.getInstallation().getSuspended()) {
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> CLIENT.get()
                        .uri(String.format("https://api.github.com/repos/%s/issues/%d",
                                integration.getRepositoryFullName(), issueNumber))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .retrieve()
                        .bodyToMono(GitHubIssue.class));
    }

    /**
     * Gets all issues from GitHub repository.
     * 
     * @param integration must not be {@literal null}; must be entity from database.
     * @return future containing list of GitHub issues.
     */
    public Flux<GitHubIssue> getIssues(GitHubIntegration integration) {
        if (integration.getInstallation().getSuspended()) {
            return Flux.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        }
        return refreshToken(integration.getInstallation())
                .flatMapMany(installation -> CLIENT.get()
                        .uri(String.format("https://api.github.com/repos/%s/issues",
                                integration.getRepositoryFullName()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .retrieve()
                        .bodyToFlux(GitHubIssue.class));
    }

    /**
     * Checks if repository with given id belongs to installation.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database.
     * @param fullName     full name of repository which is looked for.
     * @return future containing boolean value; True if repository belongs to
     *         installation.
     */
    private Mono<Boolean> hasRepository(GitHubInstallation installation, String fullName) {
        if (installation.getSuspended()) {
            return Mono.just(false);
        }
        return this.getRepositoryList(installation)
                .map(list -> {
                    for (GitHubRepository gitHubRepository : list) {
                        if (gitHubRepository.getFullName().equals(fullName)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * Retrives GitHub api repositories available for given installation.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database.
     * @return future containing list with repositories.
     */
    private Mono<List<GitHubRepository>> getRepositoryList(GitHubInstallation installation) {
        return CLIENT.get()
                .uri("https://api.github.com/installation/repositories")
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubRepositoryList.class)
                .map(GitHubRepositoryList::getRepositories);
    }

    /**
     * Checks if token of installation needs to be refreshed and refreshes it when
     * needed. When token does not need refreshing it does nothing.
     * 
     * @param installation must not be {@literal null}; must be entity from
     *                     database.
     * @return future containing updated installation.
     */
    private Mono<GitHubInstallation> refreshToken(GitHubInstallation installation) {
        return Instant.now().isAfter(installation.getExpiresAt().toInstant()) ? CLIENT.post()
                .uri(String.format("https://api.github.com/app/installations/%d/access_tokens",
                        installation.getInstallationId()))
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .header(AUTHORIZATION, BEARER + createJWT())
                .retrieve()
                .bodyToMono(InstallationToken.class)
                .map(token -> {
                    installation.updateToken(token);
                    return installationRepository.save(installation);
                }) : Mono.just(installation);
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

    /**
     * Checks if project has integration with GitHub.
     * 
     * @param project must not be {@literal null}; must be entity from database.
     * @return true if project has integration else false.
     */
    public boolean isIntegrated(Project project) {
        Optional<GitHubIntegration> integration = integrationRepository.findByProjectAndActiveNull(project);
        return integration.isPresent() && !integration.get().getInstallation().getSuspended();
    }

    /**
     * Checks if task has integration with GitHub.
     * 
     * @param project must not be {@literal null}; must be entity from database.
     * @return true if task has integration else false.
     */
    public boolean isIntegrated(Task task) {
        Optional<GitHubTask> gitHubTask = taskRepository.findByTaskAndActiveNull(task);
        return gitHubTask.isPresent() && !gitHubTask.get().getGitHubIntegration().getInstallation().getSuspended();
    }

    public Flux<Issue> getIssues(Project project) {
        Optional<GitHubIntegration> integration = integrationRepository.findByProjectAndActiveNull(project);
        if (integration.isEmpty()) {
            return Flux.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        }
        return getIssues(integration.get()).map(GitHubIssue::toIssue);
    }

    public Mono<Issue> connectIssue(Task task, Issue issue) {
        Optional<GitHubIntegration> integration = integrationRepository
                .findByProjectAndActiveNull(task.getStatus().getProject());
        if (integration.isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST));
        }
        return getIssue(integration.get(), issue.getId())
                .onErrorMap(Exception.class, e -> NotFoundRepository.getException())
                .map(gitHubIssue -> {
                    taskRepository.save(new GitHubTask(task, integration.get(), gitHubIssue.getNumber()));
                    return gitHubIssue.toIssue();
                });
    }

    public void deleteIssue(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTaskAndActiveNull(task);
        if (optional.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        GitHubTask gitHubTask = optional.get();
        gitHubTask.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        taskRepository.save(gitHubTask);
    }
}