package com.workflow.workflow.task.time;

import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.utils.FieldErrorException;

import io.swagger.v3.oas.annotations.media.Schema;

@JsonInclude(Include.NON_ABSENT)
public class TimeTrackRequest {
    @Schema(description = "Amount of seconds spent on the task. Must be greater than 0.")
    private Optional<Long> timeSpent = Optional.empty();

    public TimeTrackRequest() {
    }

    public TimeTrackRequest(Long timeSpent) {
        this.timeSpent = Optional.ofNullable(timeSpent);
    }

    public Optional<Long> getTimeSpent() {
        return timeSpent;
    }

    public void setTimeSpent(long timeSpent) {
        if (timeSpent < 1) {
            throw new FieldErrorException("timeSpent", "Time spent must be greater than 0.");
        }
        this.timeSpent = Optional.of(timeSpent);
    }
}
