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

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import kotlin.NotImplementedError;
import dev.vernite.vernite.common.utils.StateManager;
import dev.vernite.vernite.integration.git.Repository;
import dev.vernite.vernite.integration.git.github.model.Authorization;
import dev.vernite.vernite.integration.git.github.model.AuthorizationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Parameter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class GitHubController {

    private static final StateManager STATE_MANAGER = new StateManager();

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private GitHubService service;

    @Autowired
    private AuthorizationRepository authorizationRepository;

    @Autowired
    private ProjectIntegrationRepository projectIntegrationRepository;

    /**
     * Redirects user to GitHub authorization page. Should not be used as rest
     * endpoint.
     * 
     * @param user     logged in user
     * @param response response to redirect
     * @throws URISyntaxException if URI is malformed
     * @throws IOException        if redirect fails
     */
    @GetMapping("/user/integration/git/github/authorize")
    public void authorize(@NotNull @Parameter(hidden = true) User user, HttpServletResponse response)
            throws URISyntaxException, IOException {
        response.sendRedirect(service.getAuthorizationUrl(STATE_MANAGER.createState(user.getId())).toString());
    }

    /**
     * Callback for redirect from success GitHub user authorization.
     * 
     * @param code     code to get user access token
     * @param state    the state
     * @param response response
     * @return mono that will call redirect when finished
     */
    @Hidden
    @GetMapping("/user/integration/git/github/authorize_callback")
    public Mono<Void> authorizeCallback(@RequestParam String code, @RequestParam String state,
            HttpServletResponse response) {
        Mono<Authorization> result = Mono.empty();
        var id = STATE_MANAGER.retrieveState(state);

        if (id != null) {
            var user = userRepository.findById(id).orElse(null);

            if (user != null) {
                result = service.createAuthorization(user, code);
            }
        }

        return result.map(value -> "/?path=/github&status=success")
                .onErrorResume(error -> Mono.just("/?path=/github&status=error")).map(url -> {
                    try {
                        response.sendRedirect("/?path=/github&status=success");
                    } catch (IOException e) {
                        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                                "Failed to redirect to " + url, e);
                    }
                    return null;
                });
    }

    /**
     * Retrieves all GitHub authorizations for logged in user.
     * 
     * @param user logged in user
     * @return list of GitHub authorizations
     */
    @GetMapping("/user/integration/git/github")
    public List<Authorization> getAuthorizations(@NotNull @Parameter(hidden = true) User user) {
        return authorizationRepository.findByUser(user);
    }

    /**
     * Deletes GitHub authorization.
     * 
     * @param user logged in user
     * @param id   id of authorization
     */
    @DeleteMapping("/user/integration/git/github/{id}")
    public void deleteAuthorization(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        var authorization = authorizationRepository.findByIdAndUserOrThrow(id, user);
        authorizationRepository.delete(authorization);
    }

    /**
     * Retrieve repositories. Retrieves all GitHub repositories available to user.
     * 
     * @param user logged in user
     * @return list of GitHub repositories
     */
    @GetMapping("/user/integration/git/github/repository")
    public Flux<Repository> getRepositories2(@NotNull @Parameter(hidden = true) User user) {
        return service.getUserRepositories(user);
    }

    /**
     * Creates project integration. Creates project integration with GitHub
     * repository.
     * 
     * @param user       logged in user
     * @param id         id of project
     * @param repository GitHub repository
     * @return updated project
     */
    @PostMapping("/project/{id}/integration/git/github")
    public Mono<Project> createProjectIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody Repository repository) {
        var project = projectRepository.findByIdAndMemberOrThrow(id, user);
        if (!project.getGithubProjectIntegrations().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project already has GitHub integration");
        }
        return service.createProjectIntegration(user, project, repository.getFullName())
                .switchIfEmpty(Mono.error(new NotImplementedError()))
                .thenReturn(projectRepository.findByIdAndMemberOrThrow(id, user));
    }

    /**
     * Deletes project integration. Deletes project integration with GitHub
     * repository.
     * 
     * @param user      logged in user
     * @param projectId id of project
     * @param id        id of project integration
     */
    @DeleteMapping("/project/{projectId}/integration/git/github/{id}")
    public void deleteProjectIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var integration = projectIntegrationRepository.findByIdAndProjectOrThrow(id, project);
        projectIntegrationRepository.delete(integration);
    }

}
