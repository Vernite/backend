package com.workflow.workflow.workspace;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.workspace.entity.Workspace;
import com.workflow.workflow.workspace.entity.WorkspaceKey;

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
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/user/{userId}/workspace")
public class WorkspaceController {
    static final String USER_NOT_FOUND = "user not found";
    static final String WORKSPACE_NOT_FOUND = "workspace not found";

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    @Operation(summary = "Get information on all workspaces.", description = "This method returns array of all workspaces for user with given ID. Result can be empty array. Throws status 404 when user with given ID does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all user workspaces. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Workspace.class)))
            }),
            @ApiResponse(responseCode = "404", description = "User with given ID not found.", content = @Content())
    })
    @GetMapping("/")
    public List<Workspace> all(@PathVariable long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        List<Workspace> workspaces = workspaceRepository.findByUser(user);
        workspaces.sort((first, second) -> first.compareTo(second));
        return workspaces.stream().filter(w -> w.getActive() == null).toList();
    }

    @Operation(summary = "Create workspace.", description = "This method creates new workspace for user. On success returns newly created worksapce. Throws status 404 when user with given ID does not exist. Throws status 400 when sent data are incorrect.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created workspace.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User with given ID not found.", content = @Content())
    })
    @PostMapping("/")
    public Workspace add(@PathVariable long userId, @RequestBody WorkspaceRequest request) {
        if (request.getName() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "field name must exist");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        return workspaceRepository.save(new Workspace(id, user, request));
    }

    @Operation(summary = "Get workspace information.", description = "This method is used to retrive workspace with given ID for user with given user_id. On success returns workspace with given ID. Throws 404 when user or workspace does not exist. Throws 404 when workspace with given ID is not in relation with given user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workspace with given ID and user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given ID not found.", content = @Content())
    })
    @GetMapping("/{id}")
    public Workspace get(@PathVariable long userId, @PathVariable long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(new WorkspaceKey(id, user.getId()), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND));
        if (workspace.getActive() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND);
        }
        return workspace;
    }

    @Operation(summary = "Modify workspace.", description = "This method is used to modify existing workspace. On success returns modified workspace. Throws 404 when user or workspace does not exist. Throws 404 when workspace with given ID is not in relation with given user. Throws status 400 when sent data are incorrect.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified workspace with given ID.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given ID not found.", content = @Content())
    })
    @PutMapping("/{id}")
    public Workspace put(@PathVariable long userId, @PathVariable long id,
            @RequestBody WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(new WorkspaceKey(id, user.getId()), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        WORKSPACE_NOT_FOUND));
        workspace.apply(request);
        return workspaceRepository.save(workspace);
    }

    @Operation(summary = "Delete workspace.", description = "This method is used to delete workspace. On success does not return anything. Throws 404 when user or workspace does not exist. Throws 404 when workspace with given ID is not in relation with given user. Throws 400 when workspace is not empty.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Object with given ID has been deleted."),
            @ApiResponse(responseCode = "400", description = "Workspace cannot be deleted; you can delete only empty workspaces."),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given ID not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long userId, @PathVariable long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(new WorkspaceKey(id, user.getId()), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        WORKSPACE_NOT_FOUND));
        if (!projectWorkspaceRepository.findByWorkspace(workspace).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workspace not empty");
        }
        workspace.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        workspaceRepository.save(workspace);
    }
}
