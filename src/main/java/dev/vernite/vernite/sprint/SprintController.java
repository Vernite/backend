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

package dev.vernite.vernite.sprint;

import java.util.List;
import java.util.Optional;

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

import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Rest controller for performing CRUD operations on Sprint entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/sprint")
public class SprintController {

    private ProjectRepository projectRepository;

    private SprintRepository sprintRepository;

    private TaskRepository taskRepository;

    /**
     * Returns all sprints for project. If status is provided, returns only sprints
     * with given status. Otherwise returns all sprints for project. Results are
     * ordered by start date.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param status    status of sprints to return
     * @return list of sprints
     */
    @GetMapping
    public List<Sprint> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @Parameter(allowEmptyValue = true) Integer status) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return Optional.ofNullable(status)
                .map(s -> sprintRepository.findAllByProjectAndStatus(project, s.intValue()))
                .orElseGet(project::getSprints);
    }

    /**
     * Creates a new sprint for project.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param create    data for new sprint
     * @return newly created sprint
     */
    @PostMapping
    public Sprint create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateSprint create) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return sprintRepository.save(new Sprint(project, create));
    }

    /**
     * Returns sprint with given id for project.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of sprint
     * @return sprint with given id
     */
    @GetMapping("/{id}")
    public Sprint get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return sprintRepository.findByIdAndProjectOrThrow(id, project);
    }

    /**
     * Updates sprint with given id for project.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of sprint
     * @param update    data to update
     * @return updated sprint
     */
    @PutMapping("/{id}")
    public Sprint update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateSprint update) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var sprint = sprintRepository.findByIdAndProjectOrThrow(id, project);
        sprint.update(update);
        return sprintRepository.save(sprint);
    }

    /**
     * Deletes sprint with given id for project.
     * 
     * @param user      logged in user
     * @param projectId ID of project
     * @param id        ID of sprint
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var sprint = sprintRepository.findByIdAndProjectOrThrow(id, project);
        var tasks = sprint.getTasks();
        tasks.forEach(t -> t.setSprint(null));
        taskRepository.saveAll(tasks);
        sprintRepository.delete(sprint);
    }
}
