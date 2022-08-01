package com.workflow.workflow.task;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.integration.git.GitTaskService;
import com.workflow.workflow.integration.git.PullAction;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.utils.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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
    @ApiResponse(responseCode = "200", description = "List of all tasks. Can be empty.")
    @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content())
    @GetMapping
    public List<Task> all(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return taskRepository.findByStatusProjectAndActiveNullAndParentTaskNullOrderByNameAscIdAsc(project);
    }

    @Operation(summary = "Get task information", description = "This method is used to retrive status with given ID. On success returns task with given ID. Throws 404 when project or task does not exist.")
    @ApiResponse(responseCode = "200", description = "Task with given ID.")
    @ApiResponse(responseCode = "404", description = "Project or/and task with given ID not found.", content = @Content())
    @GetMapping("/{id}")
    public Task get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long id) {
        Task task = taskRepository.findByIdOrThrow(id);
        if (task.getStatus().getProject().member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        return task;
    }

    @Operation(summary = "Create task", description = "This method creates new task. On success returns newly created task.")
    @ApiResponse(responseCode = "200", description = "Newly created task.", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
    })
    @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content())
    @ApiResponse(responseCode = "404", description = "Project or status not found.", content = @Content())
    @PostMapping
    public Mono<Task> add(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody TaskRequest taskRequest) {
        if (taskRequest.getName() == null || taskRequest.getName().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name");
        }
        if (taskRequest.getDescription() == null || taskRequest.getDescription().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing description");
        }
        if (taskRequest.getStatusId() == null || taskRequest.getStatusId().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing statusId");
        }
        if (taskRequest.getType() == null || taskRequest.getType().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing type");
        }
        if (taskRequest.getPriority() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing priority");
        }
        Status status = statusRepository.findByIdOrThrow(taskRequest.getStatusId().get());
        if (status.getProject().getId() != projectId || status.getProject().member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = new Task();
        task.setCreatedAt(new Date());
        if (taskRequest.getDeadline() != null) {
            task.setDeadline(taskRequest.getDeadline().orElse(null));
        }
        if (taskRequest.getEstimatedDate() != null) {
            task.setEstimatedDate(taskRequest.getEstimatedDate().orElse(null));
        }
        task.setDescription(taskRequest.getDescription().get());
        task.setName(taskRequest.getName().get());
        if (taskRequest.getSprintId() != null && taskRequest.getSprintId().isPresent()) {
            Sprint sprint = sprintRepository.findByProjectAndNumberOrThrow(status.getProject(), taskRequest.getSprintId().get());
            task.setSprint(sprint);
        }
        task.setStatus(status);
        task.setType(taskRequest.getType().get());
        task.setUser(user);
        if (taskRequest.getAssigneeId() != null) {
            if (taskRequest.getAssigneeId().isEmpty()) {
                task.setAssignee(null);
            } else {
                Optional<User> u = userRepository.findById(taskRequest.getAssigneeId().get());
                if (u.isPresent() && task.getStatus().getProject().member(u.get()) != -1) {
                    task.setAssignee(u.get());
                } else {
                    task.setAssignee(null);
                }
            }
        }
        if (taskRequest.getParentTaskId() != null) {
            if (taskRequest.getParentTaskId().isEmpty()) {
                task.setParentTask(null);
            } else {
                Task parentTask = taskRepository.findByIdOrThrow(taskRequest.getParentTaskId().get());
                if (parentTask.getParentTask() != null) {
                    // TODO normal response status
                    throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT,
                            "parent task cant be subtask of another task or there will be possibility of cycle");
                }
                task.setParentTask(parentTask);
            }
        }
        task = taskRepository.save(task);
        if (taskRequest.getCreateIssue().isPresent() && taskRequest.getCreateIssue().get()) {
            return service.createIssue(task).then().thenReturn(task);
        } else {
            if (taskRequest.getIssue() != null) {
                switch (taskRequest.getIssue()) {
                    case ATTACH:
                        return service.connectIssue(task, taskRequest.getIssue().getIssue())
                                .switchIfEmpty(Mono.error(ObjectNotFoundException::new)).thenReturn(task);
                    case CREATE:
                        return service.createIssue(task).then().thenReturn(task);
                    case DETACH:
                        service.deleteIssue(task);
                }
            }
            if (taskRequest.getPull() != null) {
                if (taskRequest.getPull().equals(PullAction.ATTACH)) {
                    return service.connectPullRequest(task, taskRequest.getPull().getPullRequest())
                            .switchIfEmpty(Mono.error(ObjectNotFoundException::new)).thenReturn(task);
                } else {
                    service.deletePullRequest(task);
                }
            }
            return Mono.just(task);
        }
    }

    @Operation(summary = "Alter the task", description = "This method is used to modify existing task. On success returns task.")
    @ApiResponse(responseCode = "200", description = "Modified task.", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = Task.class))
    })
    @ApiResponse(responseCode = "404", description = "Task or status with given ID not found.", content = @Content())
    @PutMapping("/{id}")
    public Mono<Task> put(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody TaskRequest taskRequest) {
        Task task = taskRepository.findByIdOrThrow(id);
        if (task.getStatus().getProject().member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        if (taskRequest.getDeadline() != null) {
            task.setDeadline(taskRequest.getDeadline().orElse(null));
        }
        if (taskRequest.getEstimatedDate() != null) {
            task.setEstimatedDate(taskRequest.getEstimatedDate().orElse(null));
        }
        if (taskRequest.getDescription() != null) {
            task.setDescription(taskRequest.getDescription().get());
        }
        if (taskRequest.getName() != null) {
            task.setName(taskRequest.getName().get());
        }
        if (taskRequest.getType() != null) {
            task.setType(taskRequest.getType().get());
        }
        if (taskRequest.getPriority() != null) {
            task.setPriority(taskRequest.getPriority());
        }
        if (taskRequest.getAssigneeId() != null) {
            if (taskRequest.getAssigneeId().isEmpty()) {
                task.setAssignee(null);
            } else {
                Optional<User> u = userRepository.findById(taskRequest.getAssigneeId().get());
                if (u.isPresent() && task.getStatus().getProject().member(u.get()) != -1) {
                    task.setAssignee(u.get());
                } else {
                    task.setAssignee(null);
                }
            }
        }
        if (taskRequest.getSprintId() != null) {
            if (taskRequest.getSprintId().isEmpty()) {
                task.setSprint(null);
            } else {
                Sprint sprint = sprintRepository.findByProjectAndNumberOrThrow(task.getStatus().getProject(), taskRequest.getSprintId().get());
                task.setSprint(sprint);
            }
        }
        if (taskRequest.getStatusId() != null) {
            Status newStatus = statusRepository.findByIdOrThrow(taskRequest.getStatusId().get());
            if (projectId != newStatus.getProject().getId()) {
                throw new ObjectNotFoundException();
            }
            task.setStatus(newStatus);
        }
        if (taskRequest.getParentTaskId() != null) {
            if (taskRequest.getParentTaskId().isEmpty()) {
                task.setParentTask(null);
            } else {
                Task parentTask = taskRepository.findByIdOrThrow(taskRequest.getParentTaskId().get());
                if (parentTask.getParentTask() != null) {
                    // TODO normal response status
                    throw new ResponseStatusException(HttpStatus.I_AM_A_TEAPOT,
                            "parent task cant be subtask of another task or there will be possibility of cycle");
                }
                task.setParentTask(parentTask);
            }
        }
        taskRepository.save(task);
        if (taskRequest.getIssue() != null) {
            switch (taskRequest.getIssue()) {
                case ATTACH:
                    return service.connectIssue(task, taskRequest.getIssue().getIssue())
                            .switchIfEmpty(Mono.error(ObjectNotFoundException::new)).thenReturn(task);
                case CREATE:
                    return service.createIssue(task).then().thenReturn(task);
                case DETACH:
                    service.deleteIssue(task);
            }
        }
        if (taskRequest.getPull() != null) {
            if (taskRequest.getPull().equals(PullAction.ATTACH)) {
                return service.connectPullRequest(task, taskRequest.getPull().getPullRequest())
                        .switchIfEmpty(Mono.error(ObjectNotFoundException::new)).thenReturn(task);
            } else {
                service.deletePullRequest(task);
            }
        }
        return service.patchIssue(task).then().thenReturn(task);
    }

    @Operation(summary = "Delete task", description = "This method is used to delete task. On success does not return anything. Throws 404 when task or project does not exist.")
    @ApiResponse(responseCode = "200", description = "Task with given ID has been deleted.")
    @ApiResponse(responseCode = "404", description = "Project or task with given ID not found.")
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Task task = taskRepository.findByIdOrThrow(id);
        if (task.getStatus().getProject().member(user) == -1 || task.getStatus().getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        task.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        taskRepository.save(task);
    }
}
