package com.workflow.workflow.task;

import java.util.Date;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.workflow.workflow.integration.git.IssueAction;
import com.workflow.workflow.integration.git.IssueActionDeserializer;
import com.workflow.workflow.integration.git.PullAction;
import com.workflow.workflow.integration.git.PullActionDeserializer;
import com.workflow.workflow.status.Status;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_NULL)
public class TaskRequest {

    private Optional<String> name;
    // private Integer sprint;
    private Optional<String> description;
    private Optional<Long> statusId;
    private Optional<Long> assigneeId;
    private Optional<Integer> type;
    private Optional<Date> deadline;
    private Optional<Date> estimatedDate;
    @Deprecated
    private Optional<Boolean> createIssue = Optional.of(true);
    private Optional<Long> parentTaskId;
    @Schema(description = "Instead of word 'attach' send issue object received from other endpoint: /project/{id}/integration/git/issue")
    @JsonDeserialize(using = IssueActionDeserializer.class)
    private IssueAction issue;
    @Schema(description = "Instead of word 'attach' send pull request object received from other endpoint: /project/{id}/integration/git/pull")
    @JsonDeserialize(using = PullActionDeserializer.class)
    private PullAction pull;

    public TaskRequest() {
    }

    public TaskRequest(String name, String description, Status status, Integer type, Date deadline,
            Date estimatedDate) {
        this.name = Optional.of(name);
        this.description = Optional.of(description);
        this.statusId = Optional.of(status.getId());
        this.type = Optional.of(type);
        this.deadline = Optional.of(deadline);
        this.estimatedDate = Optional.of(estimatedDate);
    }

    public Optional<String> getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
            if (name.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be empty");
            } else if (name.length() > 50) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name cannot be longer than 50 characters");
            }
        }
        this.name = Optional.ofNullable(name);
    }

    // public Integer getSprint() {
    // return sprint;
    // }

    // public void setSprint(Integer sprint) {
    // this.sprint = sprint;
    // }

    public Optional<String> getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null) {
            description = description.trim();
        }
        this.description = Optional.ofNullable(description);
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
        this.statusId = Optional.ofNullable(statusId);
    }

    public Optional<Integer> getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = Optional.ofNullable(type);
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

    public Optional<Long> getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long parentTaskId) {
        this.parentTaskId = Optional.ofNullable(parentTaskId);
    }

    public Optional<Long> getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(Long assignee) {
        this.assigneeId = Optional.ofNullable(assignee);
    }

    public IssueAction getIssue() {
        return issue;
    }

    public void setIssue(IssueAction issueAction) {
        this.issue = issueAction;
    }

    public PullAction getPull() {
        return pull;
    }

    public void setPull(PullAction pullAction) {
        this.pull = pullAction;
    }
}
