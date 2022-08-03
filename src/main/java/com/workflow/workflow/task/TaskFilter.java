package com.workflow.workflow.task;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springdoc.api.annotations.ParameterObject;
import org.springframework.data.jpa.domain.Specification;

import com.workflow.workflow.project.Project;

import io.swagger.v3.oas.annotations.Parameter;

@ParameterObject
public class TaskFilter {
    @Parameter(description = "Id of sprint to filter by (filters are combined with 'and')")
    private Optional<Long> sprintId = Optional.empty();
    @Parameter(description = "Id of assignee to filter by (filters are combined with 'and')")
    private Optional<Long> assigneeId = Optional.empty();
    @Parameter(description = "Id of status to filter by (filters are combined with 'and')")
    private Optional<Long> statusId = Optional.empty();

    public Optional<Long> getSprintId() {
        return sprintId;
    }

    public void setSprintId(long sprintId) {
        this.sprintId = Optional.of(sprintId);
    }

    public Optional<Long> getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(long asigneeId) {
        this.assigneeId = Optional.of(asigneeId);
    }

    public Optional<Long> getStatusId() {
        return statusId;
    }

    public void setStatusId(long statusId) {
        this.statusId = Optional.of(statusId);
    }

    public Specification<Task> toSpecification(Project project) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status").get("project"), project));
            predicates.add(builder.isNull(root.get("active")));
            predicates.add(builder.isNull(root.get("parentTask")));
            sprintId.ifPresent(id -> predicates.add(builder.equal(root.get("sprint").get("number"), id)));
            assigneeId.ifPresent(id -> predicates.add(builder.equal(root.get("assignee").get("id"), id)));
            statusId.ifPresent(id -> predicates.add(builder.equal(root.get("status").get("number"), id)));
            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
