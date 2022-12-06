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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

import dev.vernite.vernite.integration.git.github.data.GitHubIntegrationInfo;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallation;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallationRepository;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegrationRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ObjectNotFoundException;

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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import reactor.core.publisher.Mono;

@RestController
public class GitHubController {
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubService service;

    @Operation(summary = "Retrieve connected GitHub accounts", description = "Retrieves all GitHub accounts connected to authenticated user account.")
    @ApiResponse(description = "List with GitHub installations. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/github")
    public List<GitHubInstallation> getInstallations(@NotNull @Parameter(hidden = true) User user) {
        return installationRepository.findByUser(user);
    }

    @Operation(summary = "Get repositories", description = "Retrieves list of repositories available to application for authenticated user. Also returns link to change settings on GitHub.")
    @ApiResponse(description = "List with repositories and link. Can be empty.", responseCode = "200", content = @Content(schema = @Schema(implementation = GitHubIntegrationInfo.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/github/repository")
    public Mono<GitHubIntegrationInfo> getRepositories(@NotNull @Parameter(hidden = true) User user) {
        return service.getRepositories(user);
    }

    @Operation(summary = "Redirect to GitHub", description = "This endpoint redirects user to GitHub to authorize application. After authorization user is redirected back to application.")
    @GetMapping("/user/integration/github/install")
    public void install(HttpServletResponse httpServletResponse) throws IOException {
        httpServletResponse.sendRedirect(GitHubService.INTEGRATION_LINK);
    }

    @Hidden
    @GetMapping("/user/integration/github/redirect")
    public Mono<Void> newInstallation(@NotNull @Parameter(hidden = true) User user,
            @RequestParam(name = "installation_id") long installationId,
            @RequestParam(name = "setup_action") String setupAction, HttpServletResponse httpServletResponse)
            throws IOException {
        httpServletResponse.sendRedirect("https://vernite.dev/?path=/github");
        if (installationRepository.findByInstallationIdAndUser(installationId, user).isPresent()) {
            return Mono.empty();
        }
        return service.newInstallation(user, installationId)
                .then();
    }

    @Operation(summary = "Delete GitHub account connection", description = "Retrieves link to delete GitHub account installation.")
    @ApiResponse(description = "Link to delete GitHub installation.", responseCode = "200", content = @Content(examples = @ExampleObject(value = "{\"link\": \"string\"}")))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Installation with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/user/integration/github/{id}")
    public Map<String, String> deleteInstallation(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        GitHubInstallation installation = installationRepository.findByIdOrThrow(id);
        if (!installation.getUser().equals(user)) {
            throw new ObjectNotFoundException();
        }
        HashMap<String, String> result = new HashMap<>();
        if ("Organization".equals(installation.getType())) {
            result.put("link", "https://github.com/organizations/" + installation.getGitHubUsername()
                    + "/settings/installations/" + installation.getInstallationId());
        } else {
            result.put("link", "https://github.com/settings/installations/" + installation.getInstallationId());
        }
        return result;
    }

    @Operation(summary = "Create GitHub repository integration", description = "Creates integration between project and GitHub repository.")
    @ApiResponse(description = "Integration created.", responseCode = "200", content = @Content(schema = @Schema(implementation = Project.class)))
    @ApiResponse(description = "Project already has integration.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or installation not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/project/{id}/integration/github")
    public Mono<Project> newIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody String repositoryFullName) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        if (project.getGitHubIntegration() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project already has connected repository.");
        }
        return service.newIntegration(user, project, repositoryFullName)
                .switchIfEmpty(Mono.error(ObjectNotFoundException::new))
                .thenReturn(projectRepository.findByIdOrThrow(id));
    }

    @Operation(summary = "Delete GitHub integration", description = "Deletes integration between project and GitHub repository.")
    @ApiResponse(description = "Integration deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or integration not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/project/{id}/integration/github")
    public void deleteIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        if (project.getGitHubIntegration() == null) {
            throw new ObjectNotFoundException();
        }
        GitHubIntegration integration = integrationRepository.findByProjectAndActiveNull(project).orElseThrow();
        integration.softDelete();
        integrationRepository.save(integration);
    }
}
