package com.workflow.workflow.workspace;

import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
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

    @Operation(summary = "This method is used to retrive array with all workspaces for given user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all user workspaces. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = Workspace.class)))
            }),
            @ApiResponse(responseCode = "404", description = "User with given id not found.", content = @Content())
    })
    @GetMapping("/")
    public Iterable<Workspace> all(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        return workspaceRepository.findByUser(user);
    }

    @Operation(summary = "This method is used create workspace for given user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created workspace.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User with given id not found.", content = @Content())
    })
    @PostMapping("/")
    public Workspace add(@PathVariable Long userId, @RequestBody WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        return workspaceRepository.save(new Workspace(request, user));
    }

    @Operation(summary = "This method is used to retrive workspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Workspace with given id and user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given id not found.", content = @Content())
    })
    @GetMapping("/{id}")
    public Workspace get(@PathVariable Long userId, @PathVariable Long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        return workspaceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND));
    }

    @Operation(summary = "This method is used to modify existing workspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified workspace with given id.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given id not found.", content = @Content())
    })
    @PatchMapping("/{id}")
    public Workspace patch(@PathVariable Long userId, @PathVariable Long id, @RequestBody WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND));
        workspace.patch(request);
        return workspaceRepository.save(workspace);
    }

    @Operation(summary = "This method is used to create or modify workspaces.")
    @Parameter(name = "id", description = "Id of object to modify. When object with given id does not exists new one is created. Id of new object may not equal given one.", in = ParameterIn.PATH, required = true, schema = @Schema(implementation = Integer.class))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified or created workspace.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User with given id not found", content = @Content())
    })
    @PutMapping("/{id}")
    public Workspace put(@PathVariable Long userId, @PathVariable Long id, @RequestBody WorkspaceRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = new Workspace(request, user);
        workspace.setId(id);
        return workspaceRepository.save(workspace);
    }

    @Operation(summary = "This method is used to delete workspace.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Object with given id has been deleted."),
            @ApiResponse(responseCode = "404", description = "Workspace or user with given id not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long userId, @PathVariable Long id) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND));
        workspaceRepository.delete(workspace);
    }
}
