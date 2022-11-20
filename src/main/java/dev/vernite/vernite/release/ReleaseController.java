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

package dev.vernite.vernite.release;

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

import dev.vernite.vernite.integration.git.GitTaskService;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/project/{projectId}/release")
public class ReleaseController {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ReleaseRepository releaseRepository;
    @Autowired
    private GitTaskService gitTaskService;

    @Operation(summary = "Retrieve all releases", description = "Retrieve all releases for a given project. Results are sorted by deadline.")
    @ApiResponse(description = "List of releases", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Release> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return releaseRepository.findAllByProjectAndActiveNullOrderByDeadlineDescName(project);
    }

    @Operation(summary = "Create a new release", description = "Create a new release for a given project.")
    @ApiResponse(description = "Release created", responseCode = "200")
    @ApiResponse(description = "Some field are not correct.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Release create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody ReleaseRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return releaseRepository.save(request.createEntity(project));
    }

    @Operation(summary = "Retrieve a release", description = "Retrieve a release for a given project.")
    @ApiResponse(description = "Release", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or release not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Release get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Release release = releaseRepository.findByIdOrThrow(id);
        if (release.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        return release;
    }

    @Operation(summary = "Update a release", description = "Update a release for a given project.")
    @ApiResponse(description = "Release updated", responseCode = "200")
    @ApiResponse(description = "Some field are not correct.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or release not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Release update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody ReleaseRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Release release = releaseRepository.findByIdOrThrow(id);
        if (release.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        release.update(request);
        return releaseRepository.save(release);
    }

    @Operation(summary = "Publish a release", description = "Publish a release for a given project.")
    @ApiResponse(description = "Release published", responseCode = "200", content = @Content(schema = @Schema(implementation = Release.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or release not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}/publish")
    public Mono<Release> publish(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Release release = releaseRepository.findByIdOrThrow(id);
        if (release.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        if (release.getReleased()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Release already published");
        }
        release.setReleased(true);
        release = releaseRepository.save(release);
        return gitTaskService.publishRelease(release).thenReturn(release);
    }

    @Operation(summary = "Delete a release", description = "Delete a release for a given project.")
    @ApiResponse(description = "Release deleted", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or release not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Release release = releaseRepository.findByIdOrThrow(id);
        if (release.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        release.softDelete();
        releaseRepository.save(release);
    }
}
