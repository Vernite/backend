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

package dev.vernite.vernite.task;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.Predicate;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import dev.vernite.vernite.utils.SoftDeleteRepository;

public interface TaskRepository extends SoftDeleteRepository<Task, Long>, JpaSpecificationExecutor<Task> {
    /**
     * Finds a task by its number and project.
     * 
     * @param project the project.
     * @param number  the number of the task.
     * @return optional of the task.
     */
    Optional<Task> findByStatusProjectAndNumberAndActiveNull(Project project, long number);

    /**
     * Finds a task by its number and project or throws error when not found.
     * 
     * @param project the project.
     * @param number  the number of the task.
     * @return the task.
     * @throws ObjectNotFoundException when not found.
     */
    default Task findByProjectAndNumberOrThrow(Project project, long number) {
        return findByStatusProjectAndNumberAndActiveNull(project, number).orElseThrow(ObjectNotFoundException::new);
    }

    /**
     * Finds tasks by specification.
     * 
     * @param spec the specification.
     * @return the tasks ordered by name and number.
     */
    default List<Task> findAllOrdered(Specification<Task> spec) {
        return findAll(spec, Sort.by(Direction.ASC, "name", "number"));
    }

    /**
     * Finds tasks by user assigned and between dates.
     * 
     * @param user      the user.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the tasks.
     */
    default List<Task> findAllFromUserAndDate(User user, Date startDate, Date endDate, EventFilter filter) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.isNotNull(root.get("assignee")));
            predicates.add(cb.equal(root.get("assignee"), user));
            if (filter.getShowEnded()) {
                predicates.add(cb.equal(root.get("status").get("isFinal"), false));
            }
            predicates.add(cb.isNull(root.get("active")));
            predicates.add(cb.or(
                    cb.and(
                            cb.isNotNull(root.get("deadline")),
                            cb.between(root.get("deadline"), startDate, endDate)),
                    cb.and(
                            cb.isNotNull(root.get("estimatedDate")),
                            cb.between(root.get("estimatedDate"), startDate, endDate))));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }

    /**
     * Finds tasks by project and between dates.
     * 
     * @param project   the project.
     * @param startDate the start date.
     * @param endDate   the end date.
     * @return the tasks.
     */
    default List<Task> findAllFromProjectAndDate(Project project, Date startDate, Date endDate, EventFilter filter) {
        return findAll((root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status").get("project"), project));
            if (filter.getShowEnded()) {
                predicates.add(cb.equal(root.get("status").get("isFinal"), false));
            }
            predicates.add(cb.isNull(root.get("active")));
            predicates.add(cb.or(
                    cb.and(
                            cb.isNotNull(root.get("deadline")),
                            cb.between(root.get("deadline"), startDate, endDate)),
                    cb.and(
                            cb.isNotNull(root.get("estimatedDate")),
                            cb.between(root.get("estimatedDate"), startDate, endDate))));
            return cb.and(predicates.toArray(new Predicate[0]));
        });
    }
}