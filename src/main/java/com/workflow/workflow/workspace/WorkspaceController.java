package com.workflow.workflow.workspace;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;

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

@RestController
@RequestMapping("/workspace")
public class WorkspaceController {

    @Autowired
    private CounterSequenceRepository counterRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Operation(summary = "Retrieve all workspaces", description = "Retrieves all workspaces for authenticated user. Results are ordered by name and id.")
    @ApiResponse(description = "List with workspaces. Can be empty.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Workspace> getAllWorkspaces(@NotNull @Parameter(hidden = true) User user) {
        return user.getWorkspaces();
    }

    @Operation(summary = "Create workspace", description = "Creates new workspace for authenticated user. All fields of request body are required.")
    @ApiResponse(description = "Newly created workspace.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Workspace newWorkspace(@NotNull @Parameter(hidden = true) User user, @RequestBody WorkspaceRequest request) {
        if (request.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name field");
        }
        if (request.getName().length() > 50 || request.getName().length() == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name field length bigger than 50 characters or empty");
        }
        long id = counterRepository.getIncrementCounter(user.getCounterSequence().getId());
        return workspaceRepository.save(new Workspace(id, user, request));
    }

    @Operation(summary = "Retrieve workspace", description = "Retrieves workspace with given id for authenticated user.")
    @ApiResponse(description = "Workspace with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Workspace getWorkspace(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        return workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
    }

    @Operation(summary = "Modify workspace", description = "Applies changes from request body to workspace with given id for authenticated user. If field from body is missing it wont be changed.")
    @ApiResponse(description = "Workspace after changes.", responseCode = "200")
    @ApiResponse(description = "Some fields failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Workspace putWorkspace(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody WorkspaceRequest request) {
        if (request.getName() != null && (request.getName().length() > 50 || request.getName().length() == 0)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name field length bigger than 50 characters or empty");
        }
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
        workspace.apply(request);
        return workspaceRepository.save(workspace);
    }

    @Operation(summary = "Delete workspace", description = "Deletes workspace with given id. Workspace to delete must be empty.")
    @ApiResponse(description = "Workspace deleted.", responseCode = "200")
    @ApiResponse(description = "Workspace with given id not empty.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void deleteWorkspace(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
        if (workspace.getProjectWorkspaces().stream().anyMatch(p -> p.getProject().getActive() == null)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workspace not empty");
        }
        workspace.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        workspaceRepository.save(workspace);
    }
}
