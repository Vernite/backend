package com.workflow.workflow.task;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.integration.git.GitTaskService;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.FieldErrorException;
import com.workflow.workflow.utils.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
@RequestMapping("/project/{projectId}/task")
public class TaskController {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private GitTaskService service;

    @Operation(summary = "Get all tasks", description = "This method returns array of all tasks for project with given ID.")
    @ApiResponse(description = "List of all tasks. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Task> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return taskRepository.findByStatusProjectAndActiveNullAndParentTaskNullOrderByNameAscIdAsc(project);
    }

    @Operation(summary = "Get task information", description = "This method is used to retrive status with given ID. On success returns task with given ID. Throws 404 when project or task does not exist.")
    @ApiResponse(description = "Task with given ID.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or/and task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Task get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long id) {
        Task task = taskRepository.findByIdOrThrow(id);
        if (task.getStatus().getProject().member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        return task;
    }

    @Operation(summary = "Create task", description = "This method creates new task. On success returns newly created task.")
    @ApiResponse(description = "Newly created task.", responseCode = "200", content = @Content(schema = @Schema(implementation = Task.class)))
    @ApiResponse(description = "Some fields are missing.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or status not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Mono<Task> create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody TaskRequest taskRequest) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        long statusId = taskRequest.getStatusId().orElseThrow(() -> new FieldErrorException("statusId", "missing"));
        Status status = statusRepository.findByProjectAndNumberOrThrow(project, statusId);
        Task task = taskRequest.createEntity(status, user);
        taskRequest.getDeadline().ifPresent(task::setDeadline);
        taskRequest.getEstimatedDate().ifPresent(task::setEstimatedDate);
        taskRequest.getSprintId().ifPresent(sprintId -> {
            Sprint sprint = null;
            if (sprintId.isPresent()) {
                sprint = sprintRepository.findByProjectAndNumberOrThrow(project, sprintId.get());
            }
            task.setSprint(sprint);
        });
        taskRequest.getAssigneeId().ifPresent(assigneeId -> {
            User assignee = null;
            if (assigneeId.isPresent()) {
                assignee = userRepository.findById(assigneeId.get()).orElse(null);
            }
            task.setAssignee(assignee);
        });
        taskRequest.getParentTaskId().ifPresent(parentTaskId -> {
            Task parentTask = null;
            if (parentTaskId.isPresent()) {
                parentTask = taskRepository.findByIdOrThrow(parentTaskId.get());
                if (parentTask.getParentTask() != null) {
                    throw new FieldErrorException("parentTaskId", "possible circular dependency");
                }
            }
            task.setParentTask(parentTask);
        });
        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        taskRequest.getCreateIssue().ifPresent(createIssue -> {
            if (Boolean.TRUE.equals(createIssue)) {
                results.add(service.createIssue(task).then());
            }
        });
        taskRequest.getIssue().ifPresent(issue -> results.add(service.handleIssueAction(issue, task).then()));
        taskRequest.getPull().ifPresent(pull -> results.add(service.handlePullAction(pull, task).then()));
        return Flux.fromIterable(results).then(Mono.just(savedTask));
    }

    @Operation(summary = "Alter the task", description = "This method is used to modify existing task. On success returns task.")
    @ApiResponse(description = "Modified task.", responseCode = "200", content = @Content(schema = @Schema(implementation = Task.class)))
    @ApiResponse(description = "Some fields are bad.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Task or status with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Mono<Task> update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody TaskRequest taskRequest) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        Task task = taskRepository.findByIdOrThrow(id);
        if (project.member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        task.update(taskRequest);
        taskRequest.getSprintId().ifPresent(sprintId -> {
            Sprint sprint = null;
            if (sprintId.isPresent()) {
                sprint = sprintRepository.findByProjectAndNumberOrThrow(project, sprintId.get());
            }
            task.setSprint(sprint);
        });
        taskRequest.getAssigneeId().ifPresent(assigneeId -> {
            User assignee = null;
            if (assigneeId.isPresent()) {
                assignee = userRepository.findById(assigneeId.get()).orElse(null);
            }
            task.setAssignee(assignee);
        });
        taskRequest.getParentTaskId().ifPresent(parentTaskId -> {
            Task parentTask = null;
            if (parentTaskId.isPresent()) {
                parentTask = taskRepository.findByIdOrThrow(parentTaskId.get());
                if (parentTask.getParentTask() != null) {
                    throw new FieldErrorException("parentTaskId", "possible circular dependency");
                }
            }
            task.setParentTask(parentTask);
        });
        taskRequest.getStatusId().ifPresent(statusId -> {
            Status status = statusRepository.findByProjectAndNumberOrThrow(project, statusId);
            task.setStatus(status);
        });
        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        taskRequest.getCreateIssue().ifPresent(createIssue -> {
            if (Boolean.TRUE.equals(createIssue)) {
                results.add(service.createIssue(task).then());
            }
        });
        taskRequest.getIssue().ifPresent(issue -> results.add(service.handleIssueAction(issue, task).then()));
        taskRequest.getPull().ifPresent(pull -> results.add(service.handlePullAction(pull, task).then()));
        return Flux.fromIterable(results).then(service.patchIssue(task).then()).thenReturn(savedTask);
    }

    @Operation(summary = "Delete task", description = "This method is used to delete task. On success does not return anything. Throws 404 when task or project does not exist.")
    @ApiResponse(description = "Task with given ID has been deleted.", responseCode = "200")
    @ApiResponse(description = "Project or task with given ID not found.", responseCode = "404")
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Task task = taskRepository.findByIdOrThrow(id);
        if (task.getStatus().getProject().member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        task.softDelete();
        taskRepository.save(task);
    }
}
