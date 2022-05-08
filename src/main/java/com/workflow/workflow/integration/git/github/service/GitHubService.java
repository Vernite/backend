package com.workflow.workflow.integration.git.github.service;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
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

    public GitHubService(GitHubInstallationRepository installationRepository) {
        this.installationRepository = installationRepository;
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
     * This method checks if repository with given id belongs to installation.
     * 
     * @param installation - installation which will be check if contains
     *                     repository.
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
