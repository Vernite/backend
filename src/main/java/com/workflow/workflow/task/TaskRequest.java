package com.workflow.workflow.task;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.workflow.workflow.integration.git.IssueAction;
import com.workflow.workflow.integration.git.IssueActionDeserializer;
import com.workflow.workflow.integration.git.PullAction;
import com.workflow.workflow.integration.git.PullActionDeserializer;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class TaskRequest {
    private static final String NULL_VALUE = "null value";
    private static final String MISSING = "missing";

    @Schema(maxLength = 50, minLength = 1, description = "The name of the task. Trailing and leading whitespaces are removed.")
    private Optional<String> name = Optional.empty();
    @Schema(description = "The description of the task. Trailing and leading whitespaces are removed.")
    private Optional<String> description = Optional.empty();
    @Schema(description = "New status of the task.")
    private Optional<Long> statusId = Optional.empty();
    @Schema(description = "New assignee of the task.")
    private Optional<Optional<Long>> assigneeId = Optional.empty();
    @Schema(description = "New type of the task.")
    private Optional<Integer> type = Optional.empty();
    @Schema(description = "New priority of the task.")
    private Optional<String> priority = Optional.empty();
    @Schema(description = "New due date of the task.")
    private Optional<Date> estimatedDate = Optional.empty();
    @Schema(description = "New deadline of the task.")
    private Optional<Date> deadline = Optional.empty();
    @Schema(description = "New parent task id.")
    private Optional<Optional<Long>> parentTaskId = Optional.empty();
    @Schema(description = "Instead of word 'attach' send issue object received from other endpoint: /project/{id}/integration/git/issue")
    @JsonDeserialize(using = IssueActionDeserializer.class)
    private Optional<IssueAction> issue = Optional.empty();
    @Schema(description = "Instead of word 'attach' send pull request object received from other endpoint: /project/{id}/integration/git/pull")
    @JsonDeserialize(using = PullActionDeserializer.class)
    private Optional<PullAction> pull = Optional.empty();
    @Schema(description = "New sprint for the task.")
    private Optional<Optional<Long>> sprintId = Optional.empty();

    /** @deprecated in favor of issue field */
    @Deprecated
    @Schema(description = "Deprecated in favor of issue field.")
    private Optional<Boolean> createIssue = Optional.of(true);

    public TaskRequest() {
    }

    public TaskRequest(String name, String description, Long statusId, Integer type, String priority) {
        this.name = Optional.ofNullable(name);
        this.description = Optional.ofNullable(description);
        this.statusId = Optional.ofNullable(statusId);
        this.type = Optional.ofNullable(type);
        this.priority = Optional.ofNullable(priority);
    }

    /**
     * Creates a new task entity from the task request.
     * 
     * @param status the status of the task.
     * @param user   the user who created the task.
     * @return the task entity.
     * @throws FieldErrorException if the task request is invalid.
     */
    public Task createEntity(Status status, User user) {
        String nameString = name.orElseThrow(() -> new FieldErrorException("name", MISSING));
        String descriptionString = description.orElseThrow(() -> new FieldErrorException("description", MISSING));
        int typeInt = type.orElseThrow(() -> new FieldErrorException("type", MISSING));
        String priorityString = priority.orElseThrow(() -> new FieldErrorException("priority", MISSING));
        return new Task(nameString, descriptionString, status, user, typeInt, priorityString);
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            throw new FieldErrorException("name", NULL_VALUE);
        }
        name = name.trim();
        if (name.isEmpty()) {
            throw new FieldErrorException("name", "empty value");
        }
        if (name.length() > 50) {
            throw new FieldErrorException("name", "too long");
        }
        this.name = Optional.of(name);
    }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description == null) {
            throw new FieldErrorException("description", NULL_VALUE);
        }
        this.description = Optional.of(description.trim());
    }

    public Optional<Date> getEstimatedDate() {
        return estimatedDate;
    }

    public void setEstimatedDate(Date estimatedDate) {
        this.estimatedDate = Optional.ofNullable(estimatedDate);
    }

    public Optional<Long> getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        if (statusId == null) {
            throw new FieldErrorException("statusId", NULL_VALUE);
        }
        this.statusId = Optional.of(statusId);
    }

    public Optional<Integer> getType() {
        return type;
    }

    public void setType(Integer type) {
        if (type == null) {
            throw new FieldErrorException("type", NULL_VALUE);
        }
        this.type = Optional.of(type);
    }

    public Optional<Date> getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = Optional.ofNullable(deadline);
    }

    public Optional<Boolean> getCreateIssue() {
        return createIssue;
    }

    public void setCreateIssue(boolean createIssue) {
        this.createIssue = Optional.ofNullable(createIssue);
    }

    public Optional<Optional<Long>> getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = Optional.of(Optional.ofNullable(parentTaskId));
    }

    public Optional<Optional<Long>> getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assignee) {
        this.assigneeId = Optional.of(Optional.ofNullable(assignee));
    }

    public Optional<IssueAction> getIssue() {
        return issue;
    }

    public void setIssue(IssueAction issueAction) {
        if (issueAction == null) {
            throw new FieldErrorException("issue", NULL_VALUE);
        }
        this.issue = Optional.of(issueAction);
    }

    public Optional<PullAction> getPull() {
        return pull;
    }

    public void setPull(PullAction pullAction) {
        if (pullAction == null) {
            throw new FieldErrorException("pull", NULL_VALUE);
        }
        this.pull = Optional.of(pullAction);
    }

    public Optional<Optional<Long>> getSprintId() {
        return sprintId;
    }

    public void setSprintId(Long sprintId) {
        this.sprintId = Optional.of(Optional.ofNullable(sprintId));
    }

    public Optional<String> getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        if (priority == null) {
            throw new FieldErrorException("priority", NULL_VALUE);
        }
        this.priority = Optional.of(priority);
    }
}
