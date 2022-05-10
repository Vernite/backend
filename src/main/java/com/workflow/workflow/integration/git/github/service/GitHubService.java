package com.workflow.workflow.integration.git.github.service;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.integration.git.github.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.GitHubTask;
import com.workflow.workflow.integration.git.github.GitHubTaskRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

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
import java.util.ArrayList;
import java.util.Date;

@Service
public class GitHubService {
    private static final String APP_ID = "195507";
    private static final String AUTHORIZATION = "Authorization";
    private static final String ACCEPT = "Accept";
    private static final String BEARER = "Bearer ";
    private static final String APPLICATION_JSON_GITHUB = "application/vnd.github.v3+json";
    private static final WebClient client = WebClient.create();
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
     * This method retrieves from GitHub api repositories available for all given
     * users installations.
     * 
     * @param user - user which repositories will be retrieved.
     * @return Future returning list with all repositories available for given user.
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
     * This method retrieves from GitHub api repositories available for given
     * installation.
     * 
     * @param installation - installation for which repositories will be retrieved.
     * @return Future returning list with all repositories available for given
     *         installation.
     */
    public Mono<List<GitHubRepository>> getRepositories(GitHubInstallation installation) {
        return refreshToken(installation)
                .flatMap(this::getRepositoryList);
    }

    /**
     * This method retrieves GitHub user information for given installation id.
     * 
     * @param installationId - id of installation which user information will be
     *                       returned.
     * @return GitHub user information for given GitHub application istallation.
     */
    public Mono<GitHubUser> getInstallationUser(long installationId) {
        return client.get()
                .uri("https://api.github.com/app/installations/" + Long.toString(installationId))
                .header(AUTHORIZATION, BEARER + createJWT())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubInstallationApi.class)
                .map(GitHubInstallationApi::getAccount);
    }

    /**
     * This method gets installation which contains repository with given id for
     * given user.
     * 
     * @param user         - user which installations will be searched.
     * @param repositoryId - id of repository which installation will be found.
     * @return Found installation object; can be null when object is not found.
     */
    public Mono<Optional<GitHubInstallation>> getRepositoryInstallation(User user, String repositoryFullName) {
        return Flux.fromIterable(installationRepository.findByUser(user))
                .filterWhen(installation -> this.hasRepository(installation, repositoryFullName))
                .reduce(Optional.empty(), (first, second) -> Optional.of(second));
    }

    /**
     * This method is used to create issue associeted with task in the system.
     * 
     * @param task - task for which issue int GitHub will be created.
     * @return - future which returns nothing
     */
    public Mono<Void> createIssue(Task task) {
        Optional<GitHubIntegration> optional = integrationRepository.findByProject(task.getStatus().getProject());
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubIntegration integration = optional.get();
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> client.post()
                        .uri(String.format("https://api.github.com/repos/%s/issues",
                                integration.getRepositoryFullName()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .bodyValue(new GitHubIssue(task))
                        .retrieve()
                        .bodyToMono(GitHubIssue.class))
                .map(issue -> taskRepository.save(new GitHubTask(task, integration, issue.getNumber())))
                .then();
    }

    /**
     * This method is used to modify issue on GitHub.
     * 
     * @param task - Task for which connected issue will be modified.
     * @return Future which returns nothing.
     */
    public Mono<Void> patchIssue(Task task) {
        Optional<GitHubTask> optional = taskRepository.findByTask(task);
        if (optional.isEmpty()) {
            return Mono.empty();
        }
        GitHubTask gitHubTask = optional.get();
        return refreshToken(gitHubTask.getGitHubIntegration().getInstallation())
                .flatMap(installation -> client.patch()
                        .uri(String.format("https://api.github.com/repos/%s/issues/%d",
                                gitHubTask.getGitHubIntegration().getRepositoryFullName(), gitHubTask.getIssueId()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .bodyValue(new GitHubIssue(task))
                        .retrieve()
                        .bodyToMono(GitHubIssue.class))
                .then();
    }

    /**
     * This method is used to get issue for integration with given number from
     * GitHub.
     * 
     * @param integration - integration for which repository will be searched for
     *                    issue.
     * @param issueNumber - number of searched issue.
     * @return Future returning issue.
     */
    public Mono<GitHubIssue> getIssue(GitHubIntegration integration, long issueNumber) {
        return refreshToken(integration.getInstallation())
                .flatMap(installation -> client.get()
                        .uri(String.format("https://api.github.com/repos/%s/issues/%d",
                                integration.getRepositoryFullName(), issueNumber))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .retrieve()
                        .bodyToMono(GitHubIssue.class));
    }

    /**
     * This method is used to get all issues from GitHub repository
     * @param integration - Integration for which issues will be returned.
     * @return Future containing list of GitHub issues.
     */
    public Flux<GitHubIssue> getIssues(GitHubIntegration integration) {
        return refreshToken(integration.getInstallation())
                .flatMapMany(installation -> client.get()
                        .uri(String.format("https://api.github.com/repos/%s/issues",
                                integration.getRepositoryFullName()))
                        .header(AUTHORIZATION, BEARER + installation.getToken())
                        .header(ACCEPT, APPLICATION_JSON_GITHUB)
                        .retrieve()
                        .bodyToFlux(GitHubIssue.class));
    }

    /**
     * This method checks if repository with given id belongs to installation.
     * 
     * @param installation       - installation which will be check if contains
     *                           repository.
     * @param repositoryFullName - full name of repository which is looked for.
     * @return Boolean value; True if repository belongs to installation.
     */
    private Mono<Boolean> hasRepository(GitHubInstallation installation, String repositoryFullName) {
        return this.getRepositoryList(installation)
                .map(list -> {
                    for (GitHubRepository gitHubRepository : list) {
                        if (gitHubRepository.getFullName().equals(repositoryFullName)) {
                            return true;
                        }
                    }
                    return false;
                });
    }

    /**
     * This method retrives GitHub api repositories available for given
     * installation.
     * 
     * @param installation - installation for which repositories will be retrieved.
     * @return Future returning list with all repositories available for given
     *         installation.
     */
    private Mono<List<GitHubRepository>> getRepositoryList(GitHubInstallation installation) {
        return client.get()
                .uri("https://api.github.com/installation/repositories")
                .header(AUTHORIZATION, BEARER + installation.getToken())
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .retrieve()
                .bodyToMono(GitHubRepositoryList.class)
                .map(GitHubRepositoryList::getRepositories);
    }

    /**
     * This method checks if token of installation needs to be refreshed and
     * refreshes it when needed. When token does not need refreshing it does
     * nothing.
     * 
     * @param installation - installation which token is checked/refreshed.
     * @return Future returning updated installation with refreshed token or when
     *         refreshing is not needed future with installation.
     */
    private Mono<GitHubInstallation> refreshToken(GitHubInstallation installation) {
        return Instant.now().isAfter(installation.getExpiresAt().toInstant()) ? client.post()
                .uri(String.format("https://api.github.com/app/installations/%d/access_tokens",
                        installation.getInstallationId()))
                .header(ACCEPT, APPLICATION_JSON_GITHUB)
                .header(AUTHORIZATION, BEARER + createJWT())
                .retrieve()
                .bodyToMono(InstallationToken.class)
                .map(token -> {
                    installation.update(token);
                    return installationRepository.save(installation);
                }) : Mono.just(installation);
    }

    /**
     * This method creates Json Web Token from file with private key. Token created
     * with this method lasts 10 minutes.
     * 
     * @return String with Json Web Token.
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
