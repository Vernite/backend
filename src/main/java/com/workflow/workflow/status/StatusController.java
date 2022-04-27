package com.workflow.workflow.status;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project/{projectId}/status")
public class StatusController {

    private static final String PROJECT_NOT_FOUND = "project not found";
    private static final String STATUS_NOT_FOUND = "status not found";

    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private StatusRepository statusRepository;

    @Operation(summary = "Retrive all statuses.", description = "This method returns array of all statuses for project with given ID. Result can be empty array. Throws status 404 when project with given ID does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all project statuses. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Status.class)))
            }),
            @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content())
    })
    @GetMapping("/")
    public Iterable<Status> all(@PathVariable long projectId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        return project.getStatuses();
    }

    @Operation(summary = "Retrive status.", description = "This method is used to retrive status with given ID. On success returns status with given ID. Throws 404 when project or status does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project with given id.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Status.class))
            }),
            @ApiResponse(responseCode = "404", description = "Project with given id not found.", content = @Content())
    })
    @GetMapping("/{id}")
    public Status get(@PathVariable long projectId, @PathVariable long id) {
        Status col = statusRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND));
        if (col.getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND);
        }
        return col;
    }

    @Operation(summary = "Delete status.", description = "This method is used to delete status. On success does not return anything. Throws 404 when status or project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status with given ID has been deleted."),
            @ApiResponse(responseCode = "404", description = "Project or status with given ID not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long projectId, @PathVariable long id) {
        Status col = statusRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND));
        if (col.getProject().getId() != projectId) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, STATUS_NOT_FOUND);
        }
        statusRepository.delete(col);
    }
}
