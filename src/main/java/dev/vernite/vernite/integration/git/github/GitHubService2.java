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
import java.util.Date;
import java.util.Optional;

import org.apache.hc.core5.net.URIBuilder;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.support.WebClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import dev.vernite.vernite.common.exception.ExternalApiException;
import dev.vernite.vernite.integration.git.Repository;
import dev.vernite.vernite.integration.git.github.api.GitHubApiClient;
import dev.vernite.vernite.integration.git.github.api.GitHubConfiguration;
import dev.vernite.vernite.integration.git.github.api.model.GitHubRepository;
import dev.vernite.vernite.integration.git.github.api.model.Installations;
import dev.vernite.vernite.integration.git.github.api.model.Repositories;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthRefreshTokenRequest;
import dev.vernite.vernite.integration.git.github.api.model.request.OauthTokenRequest;
import dev.vernite.vernite.integration.git.github.model.Authorization;
import dev.vernite.vernite.integration.git.github.model.AuthorizationRepository;
import dev.vernite.vernite.integration.git.github.model.Installation;
import dev.vernite.vernite.integration.git.github.model.InstallationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import io.jsonwebtoken.Jwts;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Service for GitHub integration.
 */
@Service
public class GitHubService2 {

    private final GitHubApiClient client;

    private GitHubConfiguration config;

    private AuthorizationRepository authorizationRepository;

    private InstallationRepository installationRepository;

    private ProjectIntegrationRepository projectIntegrationRepository;

    public GitHubService2(GitHubConfiguration config, AuthorizationRepository authorizationRepository,
            InstallationRepository installationRepository, ProjectIntegrationRepository projectIntegrationRepository) {
        this.config = config;
        this.authorizationRepository = authorizationRepository;
        this.installationRepository = installationRepository;
        this.projectIntegrationRepository = projectIntegrationRepository;

        var webClient = WebClient.builder().baseUrl(GitHubConfiguration.GITHUB_API_URL)
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

    private String createJWT() {
        var now = Instant.now();
        return Jwts.builder().setIssuedAt(Date.from(now)).setIssuer(Long.toString(config.getAppId()))
                .signWith(config.getJwtKey()).setExpiration(Date.from(now.plusSeconds(60))).compact();
    }

}
