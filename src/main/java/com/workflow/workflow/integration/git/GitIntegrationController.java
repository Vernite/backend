package com.workflow.workflow.integration.git;

import java.util.List;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.integration.git.github.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.service.GitHubService;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
public class GitIntegrationController {
    private static final String INTEGRATION_LINK = "https://github.com/apps/workflow-2022/installations/new";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubService service;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;

    @Operation(summary = "Get github application link and available repositories.", description = "This method returns link to github workflow application and list of repositories available to application for authenticated user. When list is empty authenticated user either dont have connected account or dont have any repository available for this application.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repositories list with link.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = GitHubIntegrationInfo.class))
            }),
            @ApiResponse(responseCode = "500", description = "Cannot make connection to GitHub api.", content = @Content())
    })
    @GetMapping("/integration/github")
    Mono<GitHubIntegrationInfo> getRepositories() {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return service.getRepositories(user).map(repos -> new GitHubIntegrationInfo(INTEGRATION_LINK, repos));
    }

    @Operation(summary = "Register new github connection.", description = "This method creates new GitHub appplication installation; returns link to github workflow application and list of repositories available to application for authenticated user. When list is empty authenticated user either dont have connected account or dont have any repository available for this application.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Installation created. Returns repositories list with link.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = GitHubIntegrationInfo.class))
            }),
            @ApiResponse(responseCode = "500", description = "Cannot make connection to GitHub api.", content = @Content()),
            @ApiResponse(responseCode = "503", description = "Cannot create JWT.", content = @Content())
    })
    @PostMapping("/integration/github")
    Mono<GitHubIntegrationInfo> postRepositories(long installationId) {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return service.getInstallationUser(installationId)
                .map(gitHubUser -> installationRepository
                        .save(new GitHubInstallation(installationId, user, gitHubUser.getLogin())))
                .flatMap(service::getRepositories)
                .map(repos -> new GitHubIntegrationInfo(INTEGRATION_LINK, repos));
    }

    @Operation(summary = "Get connected GitHub accounts.", description = "This method retrives all GitHub accounts associated with authenticated user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all GitHub account associated with user. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = GitHubInstallation.class)))
            })
    })
    @GetMapping("/user/integration/github")
    List<GitHubInstallation> getInstallations() {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return installationRepository.findByUser(user);
    }

    @Operation(summary = "Delete GitHub account connection.", description = "This method is used to delete GitHub account connection. On success does not return anything. Throws 404 when connection does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Object with given id has been deleted."),
            @ApiResponse(responseCode = "404", description = "Object with given id not found.")
    })
    @DeleteMapping("/user/integration/github/{id}")
    public void deleteInstallation(@PathVariable long id) {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        GitHubInstallation installation = installationRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "installation not found"));
        integrationRepository.deleteAll(integrationRepository.findByInstallation(installation));
        installationRepository.delete(installation);
    }

    @Operation(summary = "Connect GitHub repository with project.", description = "This method is used to integrate GitHub repository with project. On success does not return anything.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Repository and project are connected."),
            @ApiResponse(responseCode = "400", description = "Project with given id is already connected to GitHub repository."),
            @ApiResponse(responseCode = "404", description = "Project with given id not found. Installation for repository not found."),
            @ApiResponse(responseCode = "500", description = "Connection with GitHub api failed."),
    })
    @PostMapping("/project/{projectId}/integration/github")
    Mono<Void> createRepositoryConnection(@PathVariable long projectId, @RequestBody String repositoryFullName) {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project not found"));
        if (integrationRepository.findByProject(project).isPresent()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "given project is already integrated with github");
        }
        return service.getRepositoryInstallation(user, repositoryFullName)
                .map(installation -> installation.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "installation for repository not found")))
                .map(installation -> integrationRepository
                        .save(new GitHubIntegration(project, installation, repositoryFullName)))
                .then();
    }

    @Operation(summary = "Delete integration in project.", description = "This method is used to delete integration between GitHub repository and project. On success does not return anything.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Connection deleted."),
            @ApiResponse(responseCode = "404", description = "Project with given id not found. Integration not found.")
    })
    @DeleteMapping("/project/{projectId}/integration/github")
    void deleteRepositoryConnection(@PathVariable long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "project not found"));
        integrationRepository.delete(integrationRepository.findByProject(project)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "integration not found")));
    }
}
