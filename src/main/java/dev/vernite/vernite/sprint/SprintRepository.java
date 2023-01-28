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

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import dev.vernite.vernite.common.exception.EntityNotFoundException;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;

/**
 * CRUD repository for sprint entity.
 */
public interface SprintRepository extends CrudRepository<Sprint, Long>, JpaSpecificationExecutor<Sprint> {

    /**
     * Finds sprint by ID and project.
     * 
     * @param id      sprint ID
     * @param project project
     * @return sprint with given ID and project
     * @throws EntityNotFoundException thrown when sprint is not found or project is
     *                                 not equal to sprint project
     */
    default Sprint findByIdAndProjectOrThrow(long id, Project project) throws EntityNotFoundException {
        var sprint = findById(id).orElseThrow(() -> new EntityNotFoundException("sprint", id));
        if (sprint.getProject().getId() != project.getId()) {
            throw new EntityNotFoundException("sprint", id);
        }
        return sprint;
    }

    /**
     * Finds sprints in user projects and between dates.
     * 
     * @param user      the user.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the sprints.
     */
    default List<Sprint> findAllFromUserAndDate(User user, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> cb.and(
                cb.or(
                        cb.between(root.get("startDate"), startDate, endDate),
                        cb.between(root.get("finishDate"), startDate, endDate)),
                cb.equal(root.join("project").join("projectWorkspaces").join("workspace").join("user"), user)));
    }

    /**
     * Finds sprints in projects and between dates.
     * 
     * @param project   the project.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the sprints.
     */
    default List<Sprint> findAllFromProjectAndDate(Project project, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> cb.and(
                cb.or(
                        cb.between(root.get("startDate"), startDate, endDate),
                        cb.between(root.get("finishDate"), startDate, endDate)),
                cb.equal(root.get("project"), project)));
    }

    List<Sprint> findAllByProjectAndStatus(Project project, int status);
}
