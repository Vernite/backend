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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vernite.vernite.common.exception.ConflictStateException;
import dev.vernite.vernite.integration.git.GitTaskService;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import reactor.core.publisher.Mono;

/**
 * Rest controller for performing CRUD operations on Release entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/release")
public class ReleaseController {

    private ProjectRepository projectRepository;

    private ReleaseRepository releaseRepository;

    private GitTaskService gitTaskService;

    /**
     * Retrieves all releases for a given project. Results are sorted by deadline.
     * 
     * @param user      logged in user
     * @param projectId ID of the project
     * @return list of releases
     */
    @GetMapping
    public List<Release> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return project.getReleases();
    }

    /**
     * Creates a new release for a given project.
     * 
     * @param user      logged in user
     * @param projectId ID of the project
     * @param create    data for new release
     * @return newly created release
     */
    @PostMapping
    public Release create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateRelease create) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return releaseRepository.save(new Release(project, create));
    }

    /**
     * Retrieves a release with a given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of the project
     * @param id        ID of the release
     * @return release with given ID
     */
    @GetMapping("/{id}")
    public Release get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return releaseRepository.findByIdAndProjectOrThrow(id, project);
    }

    /**
     * Updates a release with a given ID. Performs partial update using the provided
     * data.
     * 
     * @param user      logged in user
     * @param projectId ID of the project
     * @param id        ID of the release
     * @param update    data to update
     * @return updated release
     */
    @PutMapping("/{id}")
    public Release update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateRelease update) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var release = releaseRepository.findByIdAndProjectOrThrow(id, project);
        if (release.isReleased()) {
            throw new ConflictStateException("Cannot update a released release.");
        }
        release.update(update);
        return releaseRepository.save(release);
    }

    /**
     * Publishes a release for a given project. Creates a new release on the git
     * service and updates the release with the new git release ID.
     * 
     * @param user              logged in user
     * @param projectId         ID of the project
     * @param id                ID of the release
     * @param publishGitService if true, publish the release on the git service
     * @param branch            branch to publish the release on; default is master
     * @return updated release
     */
    @PutMapping("/{id}/publish")
    public Mono<Release> publish(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, boolean publishGitService,
            @Parameter(required = false, in = ParameterIn.QUERY, description = "Not required") String branch) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var release = releaseRepository.findByIdAndProjectOrThrow(id, project);
        if (release.isReleased() && release.getGitReleaseId() > 0) {
            throw new ConflictStateException("Release already published");
        }
        for (Task task : release.getTasks()) {
            if (!task.getStatus().isFinal()) {
                throw new ConflictStateException("Release contains tasks not done");
            }
        }
        release.setReleased(true);
        var finalRelease = releaseRepository.save(release);
        if (publishGitService) {
            return gitTaskService.publishRelease(finalRelease, branch).map(gitId -> {
                finalRelease.setGitReleaseId(gitId);
                return releaseRepository.save(finalRelease);
            });
        } else {
            return Mono.just(finalRelease);
        }
    }

    /**
     * Deletes a release with a given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of the project
     * @param id        ID of the release
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var release = releaseRepository.findByIdAndProjectOrThrow(id, project);
        if (release.isReleased()) {
            throw new ConflictStateException("cannot delete release that has already been released");
        }
        releaseRepository.delete(release);
    }

}
