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

package com.workflow.workflow.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.integration.git.GitTaskService;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.time.TimeTrack;
import com.workflow.workflow.task.time.TimeTrackRepository;
import com.workflow.workflow.task.time.TimeTrackRequest;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.FieldErrorException;
import com.workflow.workflow.utils.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/project/{projectId}/task")
public class TaskController {
    private static final String PARENT_FIELD = "parentTaskId";

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
    private CounterSequenceRepository counterSequenceRepository;
    @Autowired
    private TimeTrackRepository trackRepository;
    @Autowired
    private GitTaskService service;

    /**
     * Handle the request to change sprint of a task.
     * 
     * @param sprintId the sprint id (can be null)
     * @param task     the task
     * @param project  the project
     */
    private void handleSprint(Optional<Long> sprintId, Task task, Project project) {
        Sprint sprint = null;
        if (sprintId.isPresent()) {
            sprint = sprintRepository.findByProjectAndNumberOrThrow(project, sprintId.get());
        }
        task.setSprint(sprint);
    }

    /**
     * Handle the request to change the assignee of a task.
     * 
     * @param assigneeId the assignee id (can be null)
     * @param task       the task
     */
    private void handleAssignee(Optional<Long> assigneeId, Task task) {
        User assignee = null;
        if (assigneeId.isPresent()) {
            assignee = userRepository.findById(assigneeId.get()).orElse(null);
        }
        task.setAssignee(assignee);
    }

    /**
     * Handle the request to change the parent task of a task.
     * 
     * @param parentTaskId the parent task id (can be null)
     * @param task         the task
     * @param project      the project
     */
    private void handleParent(Optional<Long> parentTaskId, Task task, Project project) {
        Task parentTask = null;
        if (parentTaskId.isPresent()) {
            parentTask = taskRepository.findByProjectAndNumberOrThrow(project, parentTaskId.get());
            if (task.getType() == Task.TaskType.USER_STORY.ordinal()) {
                if (parentTask.getType() != Task.TaskType.EPIC.ordinal()) {
                    throw new FieldErrorException(PARENT_FIELD, "user story can have only epic as parent");
                }
            } else if (task.getType() == Task.TaskType.SUBTASK.ordinal()) {
                if (parentTask.getType() == Task.TaskType.SUBTASK.ordinal()
                        || parentTask.getType() == Task.TaskType.EPIC.ordinal()) {
                    throw new FieldErrorException(PARENT_FIELD, "subtask cant have epic or subtask as parent");
                }
            } else {
                throw new FieldErrorException(PARENT_FIELD, "only user story and subtask can have parent");
            }
        }
        task.setParentTask(parentTask);
    }

    @Operation(summary = "Get all tasks", description = "This method returns array of all tasks for project with given ID.")
    @ApiResponse(description = "List of all tasks. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Task> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @ModelAttribute TaskFilter filter) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return taskRepository.findAllOrdered(filter.toSpecification(project));
    }

    @Operation(summary = "Get task information", description = "This method is used to retrive status with given ID. On success returns task with given ID. Throws 404 when project or task does not exist.")
    @ApiResponse(description = "Task with given ID.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or/and task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Task get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return taskRepository.findByProjectAndNumberOrThrow(project, id);
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
        long id = counterSequenceRepository.getIncrementCounter(project.getTaskCounter().getId());
        Task task = taskRequest.createEntity(id, status, user);
        taskRequest.getDeadline().ifPresent(task::setDeadline);
        taskRequest.getEstimatedDate().ifPresent(task::setEstimatedDate);
        taskRequest.getSprintId().ifPresent(sprintId -> handleSprint(sprintId, task, project));
        taskRequest.getAssigneeId().ifPresent(assigneeId -> handleAssignee(assigneeId, task));
        taskRequest.getParentTaskId().ifPresent(parentTaskId -> handleParent(parentTaskId, task, project));

        if (task.getType() == Task.TaskType.SUBTASK.ordinal() && task.getParentTask() == null) {
            throw new FieldErrorException(PARENT_FIELD, "subtask must have parent");
        }

        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        taskRequest.getIssue().ifPresent(issue -> results.add(service.handleIssueAction(issue, task).then()));
        taskRequest.getPull().ifPresent(pull -> results.add(service.handlePullAction(pull, task).then()));
        return Flux.concat(results).then(Mono.just(savedTask));
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
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        task.update(taskRequest);
        taskRequest.getSprintId().ifPresent(sprintId -> handleSprint(sprintId, task, project));
        taskRequest.getAssigneeId().ifPresent(assigneeId -> handleAssignee(assigneeId, task));
        taskRequest.getParentTaskId().ifPresent(parentTaskId -> handleParent(parentTaskId, task, project));
        taskRequest.getStatusId().ifPresent(statusId -> {
            Status status = statusRepository.findByProjectAndNumberOrThrow(project, statusId);
            task.setStatus(status);
        });

        if (task.getType() == Task.TaskType.SUBTASK.ordinal() && task.getParentTask() == null) {
            throw new FieldErrorException(PARENT_FIELD, "subtask must have parent");
        }

        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        taskRequest.getIssue().ifPresent(issue -> results.add(service.handleIssueAction(issue, task).then()));
        taskRequest.getPull().ifPresent(pull -> results.add(service.handlePullAction(pull, task).then()));
        return Flux.concat(results).then(service.patchIssue(task).then()).thenReturn(savedTask);
    }

    @Operation(summary = "Delete task", description = "This method is used to delete task. On success does not return anything. Throws 404 when task or project does not exist.")
    @ApiResponse(description = "Task with given ID has been deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        task.softDelete();
        taskRepository.save(task);
    }

    @Operation(summary = "Start task time tracking", description = "This method starts time tracking for task for current logged in user. On success returns tracking information.")
    @ApiResponse(description = "Tracking information.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Task is tracked already.", responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/{id}/track/start")
    public TimeTrack startTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        if (trackRepository.findByUserAndTaskAndEndDateNull(user, task).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already tracking");
        }
        return trackRepository.save(new TimeTrack(user, task));
    }

    @Operation(summary = "Stop task time tracking", description = "This method stops time tracking for task for current logged in user. On success returns tracking information.")
    @ApiResponse(description = "Tracking information.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Task is not currently tracked.", responseCode = "409", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/{id}/track/stop")
    public TimeTrack stopTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        TimeTrack track = trackRepository.findByUserAndTaskAndEndDateNull(user, task)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Not tracking"));
        track.setEndDate(new Date());
        return trackRepository.save(track);
    }

    @Operation(summary = "Manually edit time tracking", description = "This method is used to manually edit time tracking for current logged in user. On success returns tracking information. Sets edited flag to true.")
    @ApiResponse(description = "Tracking information.", responseCode = "200")
    @ApiResponse(description = "Invalid request.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}/track/{trackId}")
    public TimeTrack editTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @PathVariable long trackId, @RequestBody TimeTrackRequest trackRequest) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        TimeTrack timeTrack = trackRepository.findByIdOrThrow(trackId);
        if (timeTrack.getTask().getId() != task.getId()) {
            throw new ObjectNotFoundException();
        }
        timeTrack.update(trackRequest);
        return trackRepository.save(timeTrack);
    }

    @Operation(summary = "Delete time tracking", description = "This method is used to delete time tracking for current logged in user. On success does not return anything.")
    @ApiResponse(description = "Time tracking with given ID has been deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or task or time tracking with given ID not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}/track/{trackId}")
    public void deleteTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @PathVariable long trackId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        TimeTrack timeTrack = trackRepository.findByIdOrThrow(trackId);
        if (timeTrack.getTask().getId() != task.getId()) {
            throw new ObjectNotFoundException();
        }
        trackRepository.delete(timeTrack);
    }
}
