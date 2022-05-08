package com.workflow.workflow.task;

import java.util.Date;

public class TaskRequest {

    private String name;
    // private Integer sprint;
    private String description;
    private Long status;
    private Integer type;
    private Date deadline;
    private boolean createIssue = true;

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

    public Long getStatus() {
        return status;
    }

    public void setStatus(Long status) {
        this.status = status;
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
}
