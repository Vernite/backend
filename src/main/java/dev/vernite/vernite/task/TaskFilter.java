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
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.jpa.domain.Specification;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.task.Task.TaskType;

import io.swagger.v3.oas.annotations.Parameter;

@ParameterObject
public class TaskFilter {
    private static final String NUMBER = "number";
    private static final String STATUS = "status";
    private static final String SPRINTS = "sprints";

    @Parameter(description = "Id of sprint to filter by (filters are combined with 'and')")
    private Optional<Long> sprintId = Optional.empty();
    @Parameter(description = "Id of assignee to filter by (filters are combined with 'and')")
    private Optional<Long> assigneeId = Optional.empty();
    @Parameter(description = "Id of status to filter by (filters are combined with 'and'); multiple values are allowed")
    private Optional<List<Long>> statusId = Optional.empty();
    @Parameter(description = "Type of filtered tasks (filters are combined with 'and'); multiple values are allowed")
    private Optional<List<Integer>> type = Optional.empty();
    @Parameter(description = "Id of parent task to filter by (filters are combined with 'and')")
    private Optional<Long> parentId = Optional.empty();
    @Parameter(description = "Return only backlog tasks (filters are combined with 'and')")
    private Optional<Boolean> backlog = Optional.empty();

    public Optional<Long> getSprintId() {
        return sprintId;
    }

    public void setSprintId(long sprintId) {
        this.sprintId = Optional.of(sprintId);
    }

    public Optional<Long> getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(long assigneeId) {
        this.assigneeId = Optional.of(assigneeId);
    }

    public Optional<List<Long>> getStatusId() {
        return statusId;
    }

    public void setStatusId(List<Long> statusId) {
        this.statusId = Optional.of(statusId);
    }

    public Optional<List<Integer>> getType() {
        return type;
    }

    public void setType(List<Integer> type) {
        this.type = Optional.of(type);
    }

    public Optional<Long> getParentId() {
        return parentId;
    }

    public void setParentId(long parentId) {
        this.parentId = Optional.of(parentId);
    }

    public Optional<Boolean> getBacklog() {
        return backlog;
    }

    public void setBacklog(boolean backlog) {
        this.backlog = Optional.of(backlog);
    }

    public Specification<Task> toSpecification(Project project) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get(STATUS).get("project"), project));
            predicates.add(builder.isNull(root.get("active")));
            predicates.add(builder.notEqual(root.get("type"), TaskType.SUBTASK.ordinal()));
            sprintId.ifPresent(id -> predicates.add(builder.or(builder.in(root.join("archiveSprints").get(NUMBER)).value(id), builder.equal(root.get("sprint").get(NUMBER), id))));
            assigneeId.ifPresent(id -> predicates.add(builder.equal(root.get("assignee").get("id"), id)));
            statusId.ifPresent(ids -> predicates.add(builder.in(root.get(STATUS).get(NUMBER)).value(ids)));
            type.ifPresent(types -> predicates.add(builder.in(root.get("type")).value(types)));
            parentId.ifPresent(id -> predicates.add(builder.equal(root.get("parentTask").get(NUMBER), id)));
            backlog.ifPresent(isBacklog -> {
                if (Boolean.FALSE.equals(isBacklog)) {
                    predicates.add(builder.isNotNull(root.get("sprint")));
                } else {
                    predicates.add(builder.isNull(root.get("sprint")));
                }
            });
            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
