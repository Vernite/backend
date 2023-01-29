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

package dev.vernite.vernite.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.vernite.vernite.auditlog.AuditLog;
import dev.vernite.vernite.auditlog.AuditLogRepository;
import dev.vernite.vernite.auditlog.JsonDiff;
import dev.vernite.vernite.common.utils.counter.CounterSequenceRepository;
import dev.vernite.vernite.integration.git.GitTaskService;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.release.ReleaseRepository;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.sprint.SprintRepository;
import dev.vernite.vernite.status.StatusRepository;
import dev.vernite.vernite.task.Task.Type;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.utils.FieldErrorException;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/task")
public class TaskController {

    private static final String PARENT_FIELD = "parentTaskId";

    private MappingJackson2HttpMessageConverter converter;

    private TaskRepository taskRepository;

    private StatusRepository statusRepository;

    private AuditLogRepository auditLogRepository;

    private ProjectRepository projectRepository;

    private UserRepository userRepository;

    private SprintRepository sprintRepository;

    private ReleaseRepository releaseRepository;

    private CounterSequenceRepository counterSequenceRepository;

    private GitTaskService service;

    /**
     * Handle the request to change sprint of a task.
     * 
     * @param sprintId the sprint id (can be null)
     * @param task     the task
     * @param project  the project
     */
    private void handleSprint(Long sprintId, Task task, Project project) {
        Sprint sprint = null;
        if (sprintId != null) {
            sprint = sprintRepository.findByIdAndProjectOrThrow(sprintId, project);
            if (sprint.getStatusEnum() == Sprint.Status.CLOSED) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "cannot assign task to closed sprint");
            }
        }
        task.setSprint(sprint);
    }

    /**
     * Handle the request to change release ID of a task.
     * 
     * @param sprintId the release id (can be null)
     * @param task     the task
     * @param project  the project
     */
    private void handleReleaseId(Long releaseId, Task task, Project project) {
        Release release = null;
        if (releaseId != null) {
            release = releaseRepository.findById(releaseId)
                    .orElseThrow(() -> new ObjectNotFoundException());
            if (release.getProject().getId() != project.getId()) {
                throw new ObjectNotFoundException();
            }
        }
        task.setRelease(release);
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
            if (!task.getStatus().getProject().isMember(assignee)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid assignee");
            }
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
            if (!Type.values()[task.getType()].isValidParent(Type.values()[parentTask.getType()])) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid parent task");
            }
        }
        task.setParentTask(parentTask);
    }

    /**
     * Get all tasks for project with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param filter    filter for tasks
     * @return list of tasks
     */
    @GetMapping
    public List<Task> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @ModelAttribute TaskFilter filter) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return taskRepository.findAllOrdered(filter.toSpecification(project));
    }

    /**
     * Get task with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of task
     * @return task with given ID
     */
    @GetMapping("/{id}")
    public Task get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return taskRepository.findByProjectAndNumberOrThrow(project, id);
    }

    /**
     * Create new task.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param create    request with task data
     * @return newly created task
     */
    @PostMapping
    public Mono<Task> create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateTask create) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var status = statusRepository.findByIdAndProjectOrThrow(create.getStatusId(), project);
        var id = counterSequenceRepository.getIncrementCounter(project.getTaskCounter().getId());
        var task = new Task(id, status, user, create);

        handleSprint(create.getSprintId(), task, project);
        handleAssignee(Optional.ofNullable(create.getAssigneeId()), task);
        handleParent(Optional.ofNullable(create.getParentTaskId()), task, project);
        handleReleaseId(create.getReleaseId(), task, project);

        if (task.getType() == Task.Type.SUBTASK.ordinal() && task.getParentTask() == null) {
            throw new FieldErrorException(PARENT_FIELD, "subtask must have parent");
        }

        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        if (create.getIssue() != null) {
            results.add(service.handleIssueAction(create.getIssue(), task).then());
        }
        if (create.getPull() != null) {
            results.add(service.handlePullAction(create.getPull(), task).then());
        }
        return Flux.concat(results).then(Mono.fromRunnable(() -> {
            JsonNode newValue = converter.getObjectMapper().valueToTree(savedTask);
            AuditLog log = new AuditLog();
            log.setDate(new Date());
            log.setUser(user);
            log.setProject(project);
            log.setType("task");
            log.setOldValues(null);
            try {
                log.setNewValues(converter.getObjectMapper().writeValueAsString(newValue));
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            log.setSameValues(null);
            auditLogRepository.save(log);
        })).then(Mono.just(savedTask));
    }

    /**
     * Update task with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of task
     * @param update    request with task data
     * @return updated task
     */
    @PutMapping("/{id}")
    public Mono<Task> update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateTask update) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var task = taskRepository.findByProjectAndNumberOrThrow(project, id);
        JsonNode oldValue = converter.getObjectMapper().valueToTree(task);

        task.update(update);

        if (update.isSprintIdSet()) {
            handleSprint(update.getSprintId(), task, project);
        }

        if (update.isAssigneeIdSet()) {
            handleAssignee(Optional.ofNullable(update.getAssigneeId()), task);
        }

        if (update.isParentTaskIdSet()) {
            handleParent(Optional.ofNullable(update.getParentTaskId()), task, project);
        }

        if (update.isReleaseIdSet()) {
            handleReleaseId(update.getReleaseId(), task, project);
        }

        if (update.getStatusId() != null) {
            var status = statusRepository.findByIdAndProjectOrThrow(update.getStatusId(), project);
            task.setStatus(status);
        }

        if (task.getType() == Task.Type.SUBTASK.ordinal() && task.getParentTask() == null) {
            throw new FieldErrorException(PARENT_FIELD, "subtask must have parent");
        }

        Task savedTask = taskRepository.save(task);
        List<Mono<Void>> results = new ArrayList<>();
        if (update.getIssue() != null) {
            results.add(service.handleIssueAction(update.getIssue(), task).then());
        }
        if (update.getPull() != null) {
            results.add(service.handlePullAction(update.getPull(), task).then());
        }
        return Flux.concat(results).then(service.patchIssue(task).then()).then(Mono.fromRunnable(() -> {
            JsonNode newValue = converter.getObjectMapper().valueToTree(savedTask);
            JsonNode[] out = new JsonNode[3];
            JsonDiff.diff(oldValue, newValue, out);
            if (out[0] == null && out[1] == null) {
                return;
            }
            AuditLog log = new AuditLog();
            log.setDate(new Date());
            log.setUser(user);
            log.setProject(project);
            log.setType("task");
            try {
                log.apply(converter.getObjectMapper(), out);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            auditLogRepository.save(log);
        })).thenReturn(savedTask);
    }

    /**
     * Delete task with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of task
     * @throws JsonProcessingException thrown when JSON serialization fails
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) throws JsonProcessingException {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, id);

        JsonNode oldValue = converter.getObjectMapper().valueToTree(task);
        AuditLog log = new AuditLog();
        log.setDate(new Date());
        log.setUser(user);
        log.setProject(project);
        log.setType("task");
        log.setOldValues(converter.getObjectMapper().writeValueAsString(oldValue));
        log.setNewValues(null);
        log.setSameValues(null);
        auditLogRepository.save(log);

        taskRepository.delete(task);
    }

}
