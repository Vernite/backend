package com.workflow.workflow.task;

import java.util.Date;

public class TaskRequest {

    private String name;
    // private Integer sprint;
    private String description;
    private Long statusId;
    private Integer type;
    private Date deadline;
    private Date estimatedDate;
    private boolean createIssue = true;
    private Long parentTaskId;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    // public Integer getSprint() {
    //     return sprint;
    // }

    // public void setSprint(Integer sprint) {
    //     this.sprint = sprint;
    // }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getEstimatedDate() {
        return estimatedDate;
    }

    public void setEstimatedDate(Date estimatedDate) {
        this.estimatedDate = estimatedDate;
    }

    public Long getStatusId() {
        return statusId;
    }

    public void setStatusId(Long statusId) {
        this.statusId = statusId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public Date getDeadline() {
        return deadline;
    }

    public void setDeadline(Date deadline) {
        this.deadline = deadline;
    }

    public boolean getCreateIssue() {
        return createIssue;
    }

    public void setCreateIssue(boolean createIssue) {
        this.createIssue = createIssue;
    }

    public Long getParentTaskId() {
        return parentTaskId;
    }

    public void setParentTaskId(Long superTaskId) {
        this.parentTaskId = superTaskId;
    }
}
