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

package dev.vernite.vernite.workspace;

import java.util.List;

import javax.validation.constraints.NotNull;

import dev.vernite.vernite.counter.CounterSequenceRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ErrorType;

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
    public List<Workspace> getAll(@NotNull @Parameter(hidden = true) User user) {
        return user.getWorkspaces();
    }

    @Operation(summary = "Create workspace", description = "Creates new workspace for authenticated user. All fields of request body are required.")
    @ApiResponse(description = "Newly created workspace.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Workspace create(@NotNull @Parameter(hidden = true) User user, @RequestBody WorkspaceRequest request) {
        long id = counterRepository.getIncrementCounter(user.getCounterSequence().getId());
        return workspaceRepository.save(request.createEntity(id, user));
    }

    @Operation(summary = "Retrieve workspace", description = "Retrieves workspace with given id for authenticated user.")
    @ApiResponse(description = "Workspace with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Workspace get(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        return workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
    }

    @Operation(summary = "Modify workspace", description = "Applies changes from request body to workspace with given id for authenticated user. If field from body is missing it wont be changed.")
    @ApiResponse(description = "Workspace after changes.", responseCode = "200")
    @ApiResponse(description = "Some fields failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Workspace update(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody WorkspaceRequest request) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
        workspace.update(request);
        return workspaceRepository.save(workspace);
    }

    @Operation(summary = "Delete workspace", description = "Deletes workspace with given id. Workspace to delete must be empty.")
    @ApiResponse(description = "Workspace deleted.", responseCode = "200")
    @ApiResponse(description = "Workspace with given id not empty.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
        if (!workspace.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "workspace not empty");
        }
        workspace.softDelete();
        workspaceRepository.save(workspace);
    }
}
