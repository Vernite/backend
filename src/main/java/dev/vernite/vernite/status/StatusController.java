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

package dev.vernite.vernite.status;

import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.FieldErrorException;

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
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/status")
public class StatusController {

    private ProjectRepository projectRepository;

    private StatusRepository statusRepository;

    @Operation(summary = "Get information on all statuses", description = "This method returns array of all statuses for project with given ID.")
    @ApiResponse(responseCode = "200", description = "List of all project statuses. Can be empty.")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Status> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        return projectRepository.findByIdAndMemberOrThrow(projectId, user).getStatuses();
    }

    @Operation(summary = "Create status", description = "This method creates new status for project.")
    @ApiResponse(responseCode = "200", description = "Newly created status.")
    @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(responseCode = "404", description = "Project with given ID not found.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Status create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateStatus create) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return statusRepository.save(new Status(project, create));
    }

    @Operation(summary = "Get status information", description = "This method is used to retrieve status with given ID.")
    @ApiResponse(responseCode = "200", description = "Project with given ID.")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(responseCode = "404", description = "Project or status with given ID not found.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Status get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return statusRepository.findByIdAndProjectOrThrow(id, project);
    }

    @Operation(summary = "Alter the status", description = "This method is used to modify existing status information.")
    @ApiResponse(responseCode = "200", description = "Modified status information with given ID.")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(responseCode = "404", description = "Status or project with given ID not found.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Status update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateStatus update) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Status status = statusRepository.findByIdAndProjectOrThrow(id, project);
        status.update(update);
        return statusRepository.save(status);
    }

    @Operation(summary = "Delete status", description = "This method is used to delete status. On success does not return anything.")
    @ApiResponse(responseCode = "200", description = "Status with given ID has been deleted.")
    @ApiResponse(responseCode = "400", description = "Status is not empty.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(responseCode = "404", description = "Project or status with given ID not found.", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Status status = statusRepository.findByIdAndProjectOrThrow(id, project);
        if (!status.getTasks().isEmpty()) {
            throw new FieldErrorException("tasks", "Status has tasks assigned to it.");
        }
        statusRepository.delete(status);
    }
}
