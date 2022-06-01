package com.workflow.workflow.integration.git;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/project/{id}")
public class GitController {
    @Autowired
    GitTaskService service;
    @Autowired
    ProjectRepository projectRepository;
    @Autowired
    private TaskRepository taskRepository;

    @Operation(summary = "Retrieve git issues for project", description = "Retrieves all issues from all integrated git services for project.")
    @ApiResponse(description = "List of issues.", responseCode = "200", content = @Content(schema = @Schema(implementation = Issue.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/integration/git/issue")
    public Flux<Issue> getIssues(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return service.getIssues(project);
    }

    @Operation(summary = "Create git issue connection to task", description = "Creates new git issue connection with task. If request body is empty creates new issue. Otherwise uses existing git issue.")
    @ApiResponse(description = "Connection created.", responseCode = "200", content = @Content(schema = @Schema(implementation = Issue.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git issue not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/task/{taskId}/integration/git/issue")
    public Mono<Issue> newIssue(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId,
            @RequestBody(required = false) @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false) Issue issue) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByIdOrThrow(taskId);
        // TODO: remove when task id will be related to project id
        if (task.getStatus().getProject().getId() != project.getId()) {
            throw new ObjectNotFoundException();
        }
        if (issue == null) {
            return service.createIssue(task).last();
        }
        return service.connectIssue(task, issue);
    }

    @Deprecated
    @Operation(summary = "Create git issue connection to task", description = "Creates new git issue connection with task. If request body is empty creates new issue. Otherwise uses existing git issue.")
    @ApiResponse(description = "Connection created.", responseCode = "200", content = @Content(schema = @Schema(implementation = Issue.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git issue not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/task/{taskId}/integration/git")
    public Mono<Issue> newIssueOld(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId,
            @RequestBody @io.swagger.v3.oas.annotations.parameters.RequestBody(required = false) Issue issue) {
        return newIssue(user, id, taskId, issue);
    }

    @Operation(summary = "Delete git issue connection to task", description = "Deletes git issue connection with task. It does not delete issue on git service nor it deletes task.")
    @ApiResponse(description = "Connection deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git issue connection not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/task/{taskId}/integration/git/issue")
    void deleteIssue(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByIdOrThrow(taskId);
        // TODO: remove when task id will be related to project id
        if (task.getStatus().getProject().getId() != project.getId()) {
            throw new ObjectNotFoundException();
        }
        service.deleteIssue(task);
    }

    @Deprecated
    @Operation(summary = "Delete git issue connection to task", description = "Deletes git issue connection with task. It does not delete issue on git service nor it deletes task.")
    @ApiResponse(description = "Connection deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git issue connection not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/task/{taskId}/integration/git")
    public void deleteIssueOld(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId) {
        deleteIssue(user, id, taskId);
    }

    @Operation(summary = "Retrieve git pull requests for project", description = "Retrieves all pull requests from all integrated git services for project.")
    @ApiResponse(description = "List of pull requests.", responseCode = "200", content = @Content(schema = @Schema(implementation = PullRequest.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/integration/git/pull")
    public Flux<PullRequest> getPullRequests(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return service.getPullRequests(project);
    }

    @Operation(summary = "Create git pull request connection to task", description = "Creates new git pull request connection with task. Otherwise uses existing git pull request.")
    @ApiResponse(description = "Connection created.", responseCode = "200", content = @Content(schema = @Schema(implementation = PullRequest.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git pull request not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/task/{taskId}/integration/git/pull")
    public Mono<PullRequest> newPullRequest(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId, @RequestBody PullRequest pullRequest) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByIdOrThrow(taskId);
        // TODO: remove when task id will be related to project id
        if (task.getStatus().getProject().getId() != project.getId()) {
            throw new ObjectNotFoundException();
        }
        return service.connectPullRequest(task, pullRequest);
    }

    @Operation(summary = "Delete git pull request connection to task", description = "Deletes git pull request connection with task. It does not delete pull request on git service nor it deletes task.")
    @ApiResponse(description = "Connection deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or git pull request connection not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/task/{taskId}/integration/git/pull")
    public void deletePullRequest(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long taskId) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByIdOrThrow(taskId);
        if (task.getStatus().getProject().getId() != project.getId()) {
            throw new ObjectNotFoundException();
        }
        service.deletePullRequest(task);
    }
}
