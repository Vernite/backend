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

package dev.vernite.vernite.event;

import java.util.Date;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Model representing a generic event in calendar.
 */
@Data
@AllArgsConstructor
public class Event implements Comparable<Event> {

    /**
     * Event type enum. Used to determine what was the entity that is connected to
     * this event.
     */
    public enum Type {
        MEETING, SPRINT, TASK_ESTIMATE, TASK_DEADLINE, RELEASE
    }

    @Positive
    private long projectId;

    @JsonIgnore
    private Type type;

    @Positive
    private long relatedId;

    @NotBlank
    private String name;

    @NotNull
    private String description;

    private Date startDate;

    @NotNull
    private Date endDate;

    private String location;

    /**
     * @return the type of the event as a int (ordinal)
     */
    @JsonProperty("type")
    public int getEventType() {
        return type.ordinal();
    }

    @Override
    public int compareTo(Event o) {
        int result;
        if (startDate != null && o.startDate != null) {
            result = startDate.compareTo(o.startDate);
            if (result != 0) {
                return result;
            }
        } else if (startDate != null) {
            result = startDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
        } else if (o.startDate != null) {
            result = endDate.compareTo(o.startDate);
            if (result != 0) {
                return result;
            }
        }
        result = endDate.compareTo(o.endDate);
        if (result != 0) {
            return result;
        }
        result = Integer.compare(type.ordinal(), o.type.ordinal());
        if (result != 0) {
            return result;
        }
        result = name.compareTo(o.name);
        if (result != 0) {
            return result;
        }
        result = Long.compare(projectId, o.projectId);
        if (result != 0) {
            return result;
        }
        return Long.compare(relatedId, o.relatedId);
    }

}
