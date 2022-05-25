package com.workflow.workflow.integration.git;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.integration.git.github.GitHubService;
import com.workflow.workflow.integration.git.github.data.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.NotFoundRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import reactor.core.publisher.Mono;

@RestController
public class GitHubController {
    private static final String INTEGRATION_LINK = "https://github.com/apps/workflow-2022/installations/new";
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubService service;

    @Operation(summary = "Retrieve connected GitHub accounts.", description = "Retrieves all GitHub accounts connected to authenticated user account.")
    @ApiResponse(description = "List with GitHub installations. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/github")
    public List<GitHubInstallation> getInstallations(@NotNull @Parameter(hidden = true) User user) {
        return installationRepository.findByUser(user);
    }

    @Operation(summary = "Get repositories.", description = "Retrieves list of repositories available to application for authenticated user. Also returns link to change settings on GitHub.")
    @ApiResponse(description = "List with repositories and link. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/github/repository")
    public Mono<GitHubIntegrationInfo> getRepositories(@NotNull @Parameter(hidden = true) User user) {
        return service.getRepositories(user)
                .map(repositories -> new GitHubIntegrationInfo(INTEGRATION_LINK, repositories));
    }

    @Operation(summary = "Create GitHub account connection.", description = "Creates new GitHub appplication installation. Installation id must be retrieved from GitHub.")
    @ApiResponse(description = "GitHub installation created. List with repositories and link. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/user/integration/github")
    public Mono<GitHubIntegrationInfo> newInstallation(@NotNull @Parameter(hidden = true) User user,
            long installationId) {
        return service.getInstallationUser(installationId)
                .map(gitHubUser -> installationRepository
                        .save(new GitHubInstallation(installationId, user, gitHubUser.getLogin())))
                .flatMap(service::getRepositories)
                .map(repositories -> new GitHubIntegrationInfo(INTEGRATION_LINK, repositories));
    }

    @Operation(summary = "Delete GitHub account connection.", description = "Retrieves link to delete GitHub account installation.")
    @ApiResponse(description = "Link to delete GitHub installation.", responseCode = "200", content = @Content(examples = @ExampleObject(value = "{\"link\": \"string\"}")))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Installation with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/user/integration/github/{id}")
    public Map<String, String> deleteInstallation(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        GitHubInstallation installation = installationRepository.findByIdOrThrow(id);
        if (!installation.getUser().equals(user)) {
            throw NotFoundRepository.getException();
        }
        HashMap<String, String> result = new HashMap<>();
        result.put("link", "https://github.com/settings/installations/" + installation.getInstallationId());
        return result;
    }

    @Operation(summary = "Create GitHub repository integration.", description = "Creates integration between project and GitHub repository.")
    @ApiResponse(description = "Integration created.", responseCode = "200")
    @ApiResponse(description = "Project already has integration.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or installation not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/project/{id}/integration/github")
    public Mono<Project> newIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody String repositoryFullName) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        if (project.getGitHubIntegration() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project already has connected repository.");
        }
        return service.getRepositoryInstallation(user, repositoryFullName)
                .map(installation -> installation.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "installation for repository not found")))
                .map(installation -> integrationRepository
                        .save(new GitHubIntegration(project, installation, repositoryFullName)))
                .thenReturn(projectRepository.findByIdOrThrow(id));
    }

    @Operation(summary = "Delete GitHub integration.", description = "Deletes integration between project and GitHub repository.")
    @ApiResponse(description = "Integration deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or integration not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/project/{id}/integration/github")
    public void deleteIntegration(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        if (project.getGitHubIntegration() == null) {
            throw NotFoundRepository.getException();
        }
        GitHubIntegration integration = integrationRepository.findByProjectAndActiveNull(project).orElseThrow();
        integration.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        integrationRepository.save(integration);
    }
}
