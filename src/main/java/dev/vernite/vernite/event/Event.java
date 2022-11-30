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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import dev.vernite.vernite.meeting.Meeting;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.task.Task;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * Model representing a generic event in calendar.
 */
@ToString
@NoArgsConstructor
@EqualsAndHashCode
@AllArgsConstructor
public class Event implements Comparable<Event> {

    /**
     * Event type enum. Used to determine what was the entity that is connected to
     * this event.
     */
    public enum Type {
        MEETING, SPRINT, TASK_ESTIMATE, TASK_DEADLINE, RELEASE
    }

    /**
     * Creates a new events from a task. The events will be of type TASK_ESTIMATE
     * and TASK_DEADLINE. If the task has no estimate or deadline, the corresponding
     * event will not be created.
     * 
     * @param task the task to create the events from
     * @return a list of events
     */
    public static List<Event> from(Task task) {
        List<Event> result = new ArrayList<>();
        if (task.getEstimatedDate() != null) {
            result.add(new Event(task.getStatus().getProject().getId(), Type.TASK_ESTIMATE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getEstimatedDate(), null));
        }
        if (task.getDeadline() != null) {
            result.add(new Event(task.getStatus().getProject().getId(), Type.TASK_DEADLINE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getDeadline(), null));
        }
        return result;
    }

    /**
     * Creates a new event from a sprint. The event will be of type SPRINT.
     * 
     * @param sprint the sprint to create the event from
     * @return the event
     */
    public static Event from(Sprint sprint) {
        return new Event(sprint.getProject().getId(), Type.SPRINT, sprint.getNumber(), sprint.getName(),
                sprint.getDescription(), sprint.getStartDate(), sprint.getFinishDate(), null);
    }

    /**
     * Creates a new event from a meeting. The event will be of type MEETING.
     * 
     * @param meeting the meeting to create the event from
     * @return the event
     */
    public static Event from(Meeting meeting) {
        Event e = new Event(meeting.getProject().getId(), Type.MEETING, meeting.getId(), meeting.getName(),
                meeting.getDescription(), meeting.getStartDate(), meeting.getEndDate(), null);
        e.setLocation(meeting.getLocation());
        return e;
    }

    /**
     * Creates a new event from a release. The event will be of type RELEASE.
     * 
     * @param release the release to create the event from
     * @return the event
     */
    public static Event from(Release release) {
        return new Event(release.getProject().getId(), Type.RELEASE, release.getId(), release.getName(),
                release.getDescription(), null, release.getDeadline(), null);
    }

    @Setter
    @Getter
    @Positive
    private long projectId;

    @Setter
    @Getter
    @JsonIgnore
    private Type type;

    @Setter
    @Getter
    @Positive
    private long relatedId;

    @Setter
    @Getter
    @NotBlank
    private String name;

    @Setter
    @Getter
    @NotNull
    private String description;

    @Setter
    @Getter
    private Date startDate;

    @Setter
    @Getter
    @NotNull
    private Date endDate;

    @Setter
    @Getter
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
