package com.workflow.workflow.integration.git;

import com.workflow.workflow.integration.git.github.GitHubIntegration;
import com.workflow.workflow.integration.git.github.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.GitHubTask;
import com.workflow.workflow.integration.git.github.GitHubTaskRepository;
import com.workflow.workflow.integration.git.github.service.GitHubService;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/project/{projectId}/task/{taskId}/integration")
public class GitTaskController {
    @Autowired
    private GitHubService service;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private GitHubTaskRepository gitHubTaskRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;

    @Operation(summary = "Create new issue connection to task.", description = "This method creates new GitHub issue connection with task; when issue number is given uses existing issue; when issue number is not given creates new issue.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Issue connection with task created.", content = {
                    @Content(mediaType = "application/json", schema = @Schema())
            }),
            @ApiResponse(responseCode = "404", description = "Project, task, or github issue not found.", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Cannot make connection to GitHub api.", content = @Content()),
            @ApiResponse(responseCode = "503", description = "Cannot create JWT.", content = @Content())
    })
    @PostMapping(value = { "/github/{issueNumber}", "/github" })
    Mono<Void> createGitHubIssue(long projectId, long taskId, @PathVariable(required = false) Long issueNumber) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
        Project project = task.getStatus().getProject();
        if (projectId != project.getId()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task and project not in relation");
        }
        if (issueNumber == null) {
            return service.createIssue(task);
        } else {
            GitHubIntegration integration = integrationRepository.findByProject(project)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "project not integrated with github"));
            return service.getIssue(integration, issueNumber)
                    .onErrorMap(Exception.class,
                            e -> new ResponseStatusException(HttpStatus.NOT_FOUND, "issue not found"))
                    .map(issue -> gitHubTaskRepository.save(new GitHubTask(task, integration, issue.getNumber())))
                    .then();
        }
    }

    @Operation(summary = "Delete issue connection to task.", description = "This method deletes GitHub issue connection with task; it does not delete issue on github nor it deletes task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Issue connection with task deleted.", content = {
                    @Content(mediaType = "application/json", schema = @Schema())
            }),
            @ApiResponse(responseCode = "404", description = "Project, task, or github issue connection not found.", content = @Content())
    })
    @DeleteMapping("/github")
    void deleteGitHubIssue(long projectId, long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not found"));
        if (projectId != task.getStatus().getProject().getId()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "task and project not in relation");
        }
        gitHubTaskRepository.delete(gitHubTaskRepository.findByTask(task).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "task not integrated with github")));
    }
}
