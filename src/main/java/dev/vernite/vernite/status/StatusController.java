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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import dev.vernite.vernite.common.exception.ConflictStateException;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.AllArgsConstructor;

/**
 * Rest controller for performing CRUD operations on Status entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/status")
public class StatusController {

    private ProjectRepository projectRepository;

    private StatusRepository statusRepository;

    /**
     * Retrieves all statuses for project with given ID. Statuses will be ordered by
     * ordinal number.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @return list of statuses
     */
    @GetMapping
    public List<Status> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        return projectRepository.findByIdAndMemberOrThrow(projectId, user).getStatuses();
    }

    /**
     * Creates new status for project with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param create    data for new status
     * @return newly created status
     */
    @PostMapping
    public Status create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateStatus create) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return statusRepository.save(new Status(project, create));
    }

    /**
     * Retrieves status with given ID.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of status
     * @return status with given ID
     */
    @GetMapping("/{id}")
    public Status get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return statusRepository.findByIdAndProjectOrThrow(id, project);
    }

    /**
     * Updates status with given ID. Performs partial update using only supplied
     * fields from request body.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of status
     * @param update    data to update
     * @return updated status
     */
    @PutMapping("/{id}")
    public Status update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateStatus update) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Status status = statusRepository.findByIdAndProjectOrThrow(id, project);
        status.update(update);
        return statusRepository.save(status);
    }

    /**
     * Deletes status with given ID. Status must be empty.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of status
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Status status = statusRepository.findByIdAndProjectOrThrow(id, project);
        if (!status.getTasks().isEmpty()) {
            throw new ConflictStateException("status must be empty to delete");
        }
        statusRepository.delete(status);
    }
}
