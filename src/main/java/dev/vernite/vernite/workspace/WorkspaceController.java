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

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import dev.vernite.vernite.common.exception.ConflictStateException;
import dev.vernite.vernite.common.utils.counter.CounterSequenceRepository;
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
 * Rest controller for performing CRUD operations on Workspace entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/workspace")
public class WorkspaceController {

    private CounterSequenceRepository counterRepository;

    private WorkspaceRepository workspaceRepository;

    /**
     * Retrieves all workspaces for authenticated user. There might be extra virtual
     * workspace with ID 0 for projects that aren't contained in any workspace.
     * 
     * @param user logged in user
     * @return list with workspaces ordered by name and ID
     */
    @GetMapping
    public List<Workspace> getAll(@NotNull @Parameter(hidden = true) User user) {
        return user.getWorkspaces();
    }

    /**
     * Create new workspace for authenticated user. New workspace will have next
     * unused ID unique for user.
     * 
     * @param user   logged in user
     * @param create data for new workspace
     * @return newly created workspace
     */
    @PostMapping
    public Workspace create(@NotNull @Parameter(hidden = true) User user, @RequestBody @Valid CreateWorkspace create) {
        long id = counterRepository.getIncrementCounter(user.getCounterSequence().getId());
        return workspaceRepository.save(new Workspace(id, user, create));
    }

    /**
     * Retrieve workspace with given ID for authenticated user.
     * 
     * @param user logged in user
     * @param id   ID of workspace
     * @return workspace with given ID
     */
    @GetMapping("/{id}")
    public Workspace get(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        return workspaceRepository.findByIdOrThrow(new WorkspaceId(id, user.getId()));
    }

    /**
     * Update workspace with given ID. Performs partial update using only supplied
     * fields from request body.
     * 
     * @param user   logged in user
     * @param id     ID of workspace
     * @param update data to update
     * @return workspace after update
     */
    @PutMapping("/{id}")
    public Workspace update(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody @Valid UpdateWorkspace update) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceId(id, user.getId()));
        workspace.update(update);
        return workspaceRepository.save(workspace);
    }

    /**
     * Delete workspace with given ID. Workspace to delete must be empty.
     * 
     * @param user logged in user
     * @param id   ID of workspace
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceId(id, user.getId()));
        if (!workspace.getProjects().isEmpty()) {
            throw new ConflictStateException("workspace must be empty to delete");
        }
        workspaceRepository.delete(workspace);
    }
}
