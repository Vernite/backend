package com.workflow.workflow.integration.git;

import com.workflow.workflow.integration.git.github.GitHubService;
import com.workflow.workflow.integration.git.github.data.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
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
    private static final String PROJECT_NOT_FOUND = "project not found";
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

    @Deprecated
    @Operation(summary = "Get github application link and available repositories.", description = "Deprecaded in favor of: /user/integration/github/repository in github controller. New endpoint requires authentication.\n This method returns link to github workflow application and list of repositories available to application for authenticated user. When list is empty authenticated user either dont have connected account or dont have any repository available for this application.")
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

    @Deprecated
    @Operation(summary = "Register new github connection.", description = "Deprecaded in favor of: /user/integration/github in github controller. New endpoint requires authentication.\nThis method creates new GitHub appplication installation; returns link to github workflow application and list of repositories available to application for authenticated user. When list is empty authenticated user either dont have connected account or dont have any repository available for this application.")
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

    @Deprecated
    @Operation(summary = "Get GitHub issues for project.", description = "Deprecaded in favor of: /project/{id}/integration/git/issue in git controller. New endpoint requires authentication.\nThis method is used to get all issues from associated GitHub repository.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of github issues.", content = @Content(array = @ArraySchema(schema = @Schema(implementation = GitHubIssue.class)))),
            @ApiResponse(responseCode = "404", description = "Project with given id not found. Integration not found.", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Connection with github failed.", content = @Content())
    })
    @GetMapping("/project/{projectId}/integration/github/issue")
    Flux<GitHubIssue> getIssues(@PathVariable long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        GitHubIntegration integration = integrationRepository.findByProjectAndActiveNull(project)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "integration not found"));
        return service.getIssues(integration);
    }
}
