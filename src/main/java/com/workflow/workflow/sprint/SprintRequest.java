package com.workflow.workflow.sprint;

import java.util.Date;

public class SprintRequest {
    private String name;
    private String description;
    private Date startDate;
    private Date finishDate;
    private String status;

    public SprintRequest() {
    }

    public SprintRequest(String name, String description, Date startDate, Date finishDate, String status) {
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.finishDate = finishDate;
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name != null) {
            name = name.trim();
        }
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        if (description != null) {
            description = description.trim();
        }
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getFinishDate() {
        return finishDate;
    }

    public void setFinishDate(Date finishDate) {
        this.finishDate = finishDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
