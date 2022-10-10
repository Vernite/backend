package com.workflow.workflow.task.time;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class TimeTrackRequest {
    @Schema(description = "New start date for time track. Cant be null.")
    private Optional<Date> startDate = Optional.empty();

    @Schema(description = "New end date for time track. Cant be null.")
    private Optional<Date> endDate = Optional.empty();

    public TimeTrackRequest() {
    }

    public TimeTrackRequest(Date startDate, Date endDate) {
        this.startDate = Optional.ofNullable(startDate);
        this.endDate = Optional.ofNullable(endDate);
    }

    public Optional<Date> getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        if (startDate == null) {
            throw new FieldErrorException("startDate", "null value");
        }
        this.startDate = Optional.of(startDate);
    }

    public Optional<Date> getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        if (endDate == null) {
            throw new FieldErrorException("endDate", "null value");
        }
        this.endDate = Optional.of(endDate);
    }
}
