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

package com.workflow.workflow.task.time;

import java.util.Date;
import java.util.Optional;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;
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

    /**
     * Creates new TimeTrack object from request.
     * 
     * @param user User who created time track.
     * @param task Task for which time track is created.
     * @return New TimeTrack object.
     * @throws FieldErrorException if the task request is invalid.
     */
    public TimeTrack createEntity(User user, Task task) {
        Date start = this.startDate.orElseThrow(() -> new FieldErrorException("startDate", "missing"));
        Date end = this.endDate.orElseThrow(() -> new FieldErrorException("endDate", "missing"));
        if (start.after(end)) {
            throw new FieldErrorException("null", "end must be after start");
        }
        return new TimeTrack(user, task, start, end);
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
