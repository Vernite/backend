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

package dev.vernite.vernite.integration.git.github.model;

import java.util.List;
import java.util.Optional;

import org.springframework.data.repository.CrudRepository;

import dev.vernite.vernite.common.exception.EntityNotFoundException;
import dev.vernite.vernite.project.Project;

/**
 * CRUD repository for project integration entity.
 */
public interface ProjectIntegrationRepository extends CrudRepository<ProjectIntegration, Long> {

    /**
     * Find integration by project and id.
     * 
     * @param id      integration id
     * @param project project
     * @return integration
     * @throws EntityNotFoundException if integration not found
     */
    default ProjectIntegration findByIdAndProjectOrThrow(long id, Project project) throws EntityNotFoundException {
        var integration = findById(id).orElseThrow(() -> new EntityNotFoundException("github_project_integration", id));
        if (integration.getProject().getId() != project.getId()) {
            throw new EntityNotFoundException("github_project_integration", id);
        }
        return integration;
    }

    /**
     * Find integration by project.
     * 
     * @param project project
     * @return integration
     */
    Optional<ProjectIntegration> findByProject(Project project);

    /**
     * Find integration by repository owner and name.
     * 
     * @param owner repository owner
     * @param name  repository name
     * @return integration
     */
    List<ProjectIntegration> findByRepositoryOwnerAndRepositoryName(String owner, String name);

}
