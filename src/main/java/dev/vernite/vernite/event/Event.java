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

import org.springframework.data.annotation.ReadOnlyProperty;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.vernite.vernite.meeting.Meeting;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.task.Task;

public class Event implements Comparable<Event> {
    public enum EventType {
        MEETING, SPRINT, TASK_ESTIMATE, TASK_DEADLINE
    }

    private long projectId;
    private EventType type;
    private long relatedId;
    private String name;
    private String description;
    private Date startDate;
    private Date endDate;
    private String location;

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
            result.add(new Event(task.getStatus().getProject().getId(), EventType.TASK_ESTIMATE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getEstimatedDate()));
        }
        if (task.getDeadline() != null) {
            result.add(new Event(task.getStatus().getProject().getId(), EventType.TASK_DEADLINE, task.getNumber(),
                    task.getName(), task.getDescription(), null, task.getDeadline()));
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
        return new Event(sprint.getProject().getId(), EventType.SPRINT, sprint.getNumber(), sprint.getName(),
                sprint.getDescription(), sprint.getStartDate(), sprint.getFinishDate());
    }

    /**
     * Creates a new event from a meeting. The event will be of type MEETING.
     * 
     * @param meeting the meeting to create the event from
     * @return the event
     */
    public static Event from(Meeting meeting) {
        Event e = new Event(meeting.getProject().getId(), EventType.MEETING, meeting.getId(), meeting.getName(),
                meeting.getDescription(), meeting.getStartDate(), meeting.getEndDate());
        e.setLocation(meeting.getLocation());
        return e;
    }

    private Event(long projectId, EventType type, long relatedId, String name, String description, Date startDate,
            Date endDate) {
        this.projectId = projectId;
        this.type = type;
        this.relatedId = relatedId;
        this.name = name;
        this.description = description;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public long getProjectId() {
        return projectId;
    }

    public void setProjectId(long projectId) {
        this.projectId = projectId;
    }

    public int getType() {
        return type.ordinal();
    }

    @JsonIgnore
    public EventType getEventType() {
        return type;
    }

    @ReadOnlyProperty
    public void setType(EventType type) {
        this.type = type;
    }

    public long getRelatedId() {
        return relatedId;
    }

    public void setRelatedId(long relatedId) {
        this.relatedId = relatedId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    @Override
    public int compareTo(Event o) {
        int result;
        if (startDate != null && o.startDate != null) {
            result = startDate.compareTo(o.startDate);
            if (result != 0) {
                return result;
            }
            result = endDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
        } else if (startDate != null) {
            result = startDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
            result = endDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
        } else if (o.startDate != null) {
            result = endDate.compareTo(o.startDate);
            if (result != 0) {
                return result;
            }
            result = endDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
        } else {
            result = endDate.compareTo(o.endDate);
            if (result != 0) {
                return result;
            }
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
