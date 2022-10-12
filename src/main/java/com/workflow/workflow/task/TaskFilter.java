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
import com.workflow.workflow.task.Task.TaskType;

import io.swagger.v3.oas.annotations.Parameter;

@ParameterObject
public class TaskFilter {
    @Parameter(description = "Id of sprint to filter by (filters are combined with 'and')")
    private Optional<Long> sprintId = Optional.empty();
    @Parameter(description = "Id of assignee to filter by (filters are combined with 'and')")
    private Optional<Long> assigneeId = Optional.empty();
    @Parameter(description = "Id of status to filter by (filters are combined with 'and'); multiple values are allowed")
    private Optional<List<Long>> statusId = Optional.empty();
    @Parameter(description = "Type of filtered tasks (filters are combined with 'and'); multiple values are allowed")
    private Optional<List<Integer>> type = Optional.empty();

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

    public Optional<List<Long>> getStatusId() {
        return statusId;
    }

    public void setStatusId(List<Long> statusId) {
        this.statusId = Optional.of(statusId);
    }

    public Specification<Task> toSpecification(Project project) {
        return (Root<Task> root, CriteriaQuery<?> query, CriteriaBuilder builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(builder.equal(root.get("status").get("project"), project));
            predicates.add(builder.isNull(root.get("active")));
            predicates.add(builder.notEqual(root.get("type"), TaskType.SUBTASK.ordinal()));
            sprintId.ifPresent(id -> predicates.add(builder.equal(root.get("sprint").get("number"), id)));
            assigneeId.ifPresent(id -> predicates.add(builder.equal(root.get("assignee").get("id"), id)));
            statusId.ifPresent(ids -> predicates.add(builder.in(root.get("status").get("number")).value(ids)));
            type.ifPresent(types -> predicates.add(builder.in(root.get("type")).value(types)));
            return builder.and(predicates.toArray(new Predicate[predicates.size()]));
        };
    }
}
