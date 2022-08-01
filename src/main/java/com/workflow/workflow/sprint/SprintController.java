package com.workflow.workflow.sprint;

import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.FieldErrorException;
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
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    @Operation(summary = "Retrieve all sprints", description = "Retrieves all sprints for project. Results are ordered by id.")
    @ApiResponse(description = "List with sprints. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Sprint> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
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
    public Sprint create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody SprintRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        long id = counterSequenceRepository.getIncrementCounter(project.getSprintCounter().getId());
        return sprintRepository.save(request.createEntity(id, project));
    }

    @Operation(summary = "Retrieve a sprint", description = "Retrieves a sprint for project.")
    @ApiResponse(description = "Sprint with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Sprint get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return sprintRepository.findByProjectAndNumberOrThrow(project, id);
    }

    @Operation(summary = "Update a sprint", description = "Updates a sprint for project.")
    @ApiResponse(description = "Sprint updated.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Sprint update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody SprintRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Sprint sprint = sprintRepository.findByProjectAndNumberOrThrow(project, id);

        request.getStartDate().ifPresent(start -> {
            if (request.getFinishDate().isEmpty() && start.after(sprint.getFinishDate())) {
                throw new FieldErrorException("startDate", BAD_DATE);
            }
        });

        request.getFinishDate().ifPresent(finish -> {
            if (request.getStartDate().isEmpty() && finish.before(sprint.getStartDate())) {
                throw new FieldErrorException("finishDate", BAD_DATE);

            }
        });

        sprint.update(request);
        return sprintRepository.save(sprint);
    }

    @Operation(summary = "Delete a sprint", description = "Deletes a sprint for project.")
    @ApiResponse(description = "Sprint deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or sprint not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Sprint sprint = sprintRepository.findByProjectAndNumberOrThrow(project, id);
        sprint.softDelete();
        sprintRepository.save(sprint);
    }
}
