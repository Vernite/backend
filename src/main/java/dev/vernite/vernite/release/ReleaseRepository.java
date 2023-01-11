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

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.CrudRepository;

import dev.vernite.vernite.common.exception.EntityNotFoundException;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;

/**
 * CRUD repository for release entity.
 */
public interface ReleaseRepository extends CrudRepository<Release, Long>, JpaSpecificationExecutor<Release> {

    /**
     * Find release by ID and project.
     * 
     * @param id      release ID
     * @param project project
     * @return release with given ID and project
     * @throws EntityNotFoundException thrown when release is not found or project
     *                                 is not equal to release project
     */
    default Release findByIdAndProjectOrThrow(long id, Project project) {
        var release = findById(id).orElseThrow(() -> new EntityNotFoundException("release", id));
        if (release.getProject().getId() != project.getId()) {
            throw new EntityNotFoundException("release", id);
        }
        return release;
    }

    /**
     * Find all releases from user and date range.
     * 
     * @param user      user
     * @param startDate start date
     * @param endDate   end date
     * @return list of releases
     */
    default List<Release> findAllFromUserAndDate(User user, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> cb.and(
                cb.between(root.get("deadline"), startDate, endDate),
                cb.equal(root.join("project").join("projectWorkspaces").join("workspace").join("user"), user)));
    }

    /**
     * Find all releases from project and date range.
     * 
     * @param project   project
     * @param startDate start date
     * @param endDate   end date
     * @return list of releases
     */
    default List<Release> findAllFromProjectAndDate(Project project, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> {
            return cb.and(cb.equal(root.get("project"), project),
                    cb.between(root.get("deadline"), startDate, endDate));
        });
    }

}
