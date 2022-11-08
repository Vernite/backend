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

package com.workflow.workflow.meeting;

import java.util.Date;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.SoftDeleteRepository;

public interface MeetingRepository extends SoftDeleteRepository<Meeting, Long>, JpaSpecificationExecutor<Meeting> {
    /**
     * Finds a meeting by its id and project sorted by date.
     * 
     * @param project the project.
     * @return list of meetings.
     */
    default List<Meeting> findAllByProjectAndActiveNullSorted(Project project) {
        return findAll((root, query, cb) -> {
            return cb.equal(root.get("project"), project);
        }, Sort.by(Direction.ASC, "startDate", "endDate", "name"));
    }

    /**
     * Finds a meeting by project and between dates.
     * 
     * @param project   the project.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return list of meetings.
     */
    default List<Meeting> findMeetingsByProjectAndDate(Project project, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> {
            return cb.and(cb.equal(root.get("project"), project),
                    cb.between(root.get("startDate"), startDate, endDate));
        });
    }

    /**
     * Finds a meeting by user and between dates.
     * 
     * @param user      the user.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return list of meetings.
     */
    default List<Meeting> findMeetingsByUserAndDate(User user, Date startDate, Date endDate) {
        return findAll((root, query, cb) -> {
            return cb.and(cb.equal(root.join("participants"), user),
                    cb.between(root.get("startDate"), startDate, endDate));
        });
    }
}
