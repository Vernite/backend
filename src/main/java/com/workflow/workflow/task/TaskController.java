package com.workflow.workflow.task;

import java.util.Date;

import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project/{projectId}/task")
public class TaskController {

    private static final String TASK_NOT_FOUND = "task not found";
    private static final String STATUS_NOT_FOUND = "status not found";
    private static final String STATUS_AND_PROJECT_NOT_RELATION = "status is not in relation with given project";

    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Create task.", description = "This method creates new task. On success returns newly created task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created task.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskRequest.class))
            }),
            @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Project or status not found.", content = @Content())
    })
    @PostMapping("/")
    public Task add(@PathVariable long projectId, @RequestBody TaskRequest taskRequest) {
        if (taskRequest.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name");
        }
        if (taskRequest.getDescription() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing description");
        }
        if (taskRequest.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing status");
        }
        if (taskRequest.getType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing type");
        }
        Task task = new Task();
        task.setCreatedAt(new Date());
        task.setDeadline(taskRequest.getDeadline());
        task.setDescription(taskRequest.getDescription());
        task.setName(taskRequest.getName());
        // TODO sprint update
        task.setStatus(statusRepository.findById(taskRequest.getStatus()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND)));
        task.setType(taskRequest.getType());
        task.setUser(userRepository.findById(1L).orElseThrow());
        if (task.getStatus().getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_AND_PROJECT_NOT_RELATION);
        }
        return taskRepository.save(task);
    }

    @Operation(summary = "Alter the task.", description = "This method is used to modify existing task. On success returns task.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified task.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = TaskRequest.class))
            }),
            @ApiResponse(responseCode = "404", description = "Task or status with given ID not found.", content = @Content())
    })
    @PutMapping("/{id}")
    public Task put(@PathVariable long projectId, @PathVariable long id, @RequestBody TaskRequest taskRequest) {
        Task task = taskRepository.findById(id).orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND));
        if (task.getStatus().getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND);
        }
        if (taskRequest.getDeadline() != null) {
            task.setDeadline(taskRequest.getDeadline());
        }
        if (taskRequest.getDescription() != null) {
            task.setDescription(taskRequest.getDescription());
        }
        if (taskRequest.getName() != null) {
            task.setName(taskRequest.getName());
        }
        if (taskRequest.getType() != null) {
            task.setType(taskRequest.getType());
        }
        // TODO sprint update
        
        Status newStatus = statusRepository.findById(taskRequest.getStatus()).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND));
        if (projectId != newStatus.getProject().getId()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_AND_PROJECT_NOT_RELATION);
        }
        task.setStatus(newStatus);
        
        taskRepository.save(task);
        return task;
    }

    @Operation(summary = "Delete task.", description = "This method is used to delete task. On success does not return anything. Throws 404 when task or project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task with given ID has been deleted."),
            @ApiResponse(responseCode = "404", description = "Project or task with given ID not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long projectId, @PathVariable long id) {
        Task task = taskRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND));
        if (task.getStatus().getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND);
        }
        taskRepository.delete(task);
    }
}
