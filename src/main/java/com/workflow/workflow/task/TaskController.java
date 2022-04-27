package com.workflow.workflow.task;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project/{projectId}/task")
public class TaskController {

    private static final String TASK_NOT_FOUND = "task not found";

    @Autowired
    private TaskRepository taskRepository;

    @Operation(summary = "Delete task.", description = "This method is used to delete task. On success does not return anything. Throws 404 when task or project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Task with given ID has been deleted."),
            @ApiResponse(responseCode = "404", description = "Project or task with given ID not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long projectId, @PathVariable long id) {
        Task col = taskRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND));
        if (col.getStatus().getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, TASK_NOT_FOUND);
        }
        taskRepository.delete(col);
    }
}
