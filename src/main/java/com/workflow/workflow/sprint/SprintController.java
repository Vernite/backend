package com.workflow.workflow.sprint;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

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

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.ObjectNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/project/{projectId}/sprint")
public class SprintController {
    private static final String BAD_DATE = "start date after finish date";
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private SprintRepository sprintRepository;

    @Operation(summary = "Retrieve all sprints", description = "Retrieves all sprints for project. Results are ordered by name and id.")
    @ApiResponse(description = "List with sprints. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Sprint> allSprints(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return project.getSprints();
    }

    @Operation(summary = "Create a sprint", description = "Creates a new sprint for project.")
    @ApiResponse(description = "Sprint created.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Sprint newSprint(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody SprintRequest request) {
        if (request.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name field");
        }
        if (request.getName().length() > 50 || request.getName().length() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name field length bigger than 50 characters or empty");
        }
        if (request.getStartDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing start date field");
        }
        if (request.getFinishDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing finish date field");
        }
        if (request.getStartDate().after(request.getFinishDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_DATE);
        }
        if (request.getStatus() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing status field");
        }
        if (request.getDescription() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing description field");
        }
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return sprintRepository.save(new Sprint(request, project));
    }

    @Operation(summary = "Retrieve a sprint", description = "Retrieves a sprint for project.")
    @ApiResponse(description = "Sprint with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Sprint getSprint(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return sprintRepository.findByIdOrThrow(id);
    }

    @Operation(summary = "Update a sprint", description = "Updates a sprint for project.")
    @ApiResponse(description = "Sprint updated.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Sprint putSprint(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody SprintRequest request) {
        if (request.getName() != null && (request.getName().length() > 50 || request.getName().length() == 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "name field length bigger than 50 characters or empty");
        }
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Sprint sprint = sprintRepository.findByIdOrThrow(id);
        if (sprint.getProject() != project) {
            throw new ObjectNotFoundException();
        }

        if (request.getStartDate() != null && request.getFinishDate() != null) {
            if (request.getStartDate().after(request.getFinishDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_DATE);
            }
        } else if (request.getStartDate() != null) {
            if (request.getStartDate().after(sprint.getFinishDate())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_DATE);
            }
        } else if (request.getFinishDate() != null && request.getFinishDate().before(sprint.getStartDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, BAD_DATE);
        }
        sprint.apply(request);
        return sprintRepository.save(sprint);
    }

    @Operation(summary = "Delete a sprint", description = "Deletes a sprint for project.")
    @ApiResponse(description = "Sprint deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void deleteSprint(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Sprint sprint = sprintRepository.findByIdOrThrow(id);
        if (sprint.getProject() != project) {
            throw new ObjectNotFoundException();
        }
        sprint.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        sprintRepository.save(sprint);
    }
}
