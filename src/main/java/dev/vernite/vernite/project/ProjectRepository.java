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

package dev.vernite.vernite.project;

import org.springframework.stereotype.Repository;

import dev.vernite.vernite.common.exception.EntityNotFoundException;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.SoftDeleteRepository;

/**
 * CRUD repository for project entity.
 */
@Repository
public interface ProjectRepository extends SoftDeleteRepository<Project, Long> {

    /**
     * Finds active project by id and checks if user is member of project.
     * 
     * @param id   project id
     * @param user potential project member
     * @return project with given id and user is its member
     * @throws EntityNotFoundException thrown when project is not found or user is
     *                                 not member of found project
     */
    default Project findByIdAndMemberOrThrow(long id, User user) throws EntityNotFoundException {
        Project project = findByIdAndActiveNull(id).orElseThrow(() -> new EntityNotFoundException("project", id));
        if (!project.isMember(user)) {
            throw new EntityNotFoundException("project", id);
        }
        return project;
    }

}
