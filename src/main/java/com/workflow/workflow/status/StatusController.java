package com.workflow.workflow.status;

import java.util.List;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
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
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/project/{projectId}/status")
public class StatusController {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    @Operation(summary = "Get information on all statuses", description = "This method returns array of all statuses for project with given ID. Result can be empty array. Throws status 404 when project with given ID does not exist.")
    @ApiResponse(responseCode = "200", description = "List of all project statuses. Can be empty.")
    @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content())
    @GetMapping
    public List<Status> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return project.getStatuses();
    }

    @Operation(summary = "Create status", description = "This method creates new status for project. On success returns newly created status. Throws status 404 when user with given ID does not exist. Throws status 400 when sent data are incorrect.")
    @ApiResponse(responseCode = "200", description = "Newly created status.")
    @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content())
    @ApiResponse(responseCode = "404", description = "User with given ID not found.", content = @Content())
    @PostMapping
    public Status create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody StatusRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        long id = counterSequenceRepository.getIncrementCounter(project.getStatusCounter().getId());
        return statusRepository.save(request.createEntity(id, project));
    }

    @Operation(summary = "Get status information", description = "This method is used to retrive status with given ID. On success returns status with given ID. Throws 404 when project or status does not exist.")
    @ApiResponse(responseCode = "200", description = "Project with given ID.")
    @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content())
    @GetMapping("/{id}")
    public Status get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return statusRepository.findByProjectAndNumberOrThrow(project, id);
    }

    @Operation(summary = "Alter the status", description = "This method is used to modify existing status information. On success returns modified status. Throws 404 when project or status does not exist or when workspace with given ID is not in relation with given project.")
    @ApiResponse(responseCode = "200", description = "Modified status information with given ID.")
    @ApiResponse(responseCode = "404", description = "Status or project with given ID not found.", content = @Content())
    @PutMapping("/{id}")
    public Status update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody StatusRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Status status = statusRepository.findByProjectAndNumberOrThrow(project, id);
        status.update(request);
        return statusRepository.save(status);
    }

    @Operation(summary = "Delete status", description = "This method is used to delete status. On success does not return anything. Throws 404 when status or project does not exist.")
    @ApiResponse(responseCode = "200", description = "Status with given ID has been deleted.")
    @ApiResponse(responseCode = "404", description = "Project or status with given ID not found.")
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Status status = statusRepository.findByProjectAndNumberOrThrow(project, id);
        status.softDelete();
        statusRepository.save(status);
    }
}
