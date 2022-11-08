package com.workflow.workflow.event;

import java.util.List;
import java.util.Optional;

import org.springdoc.api.annotations.ParameterObject;

import com.workflow.workflow.event.Event.EventType;

import io.swagger.v3.oas.annotations.media.Schema;

@ParameterObject
public class EventFilter {
    @Schema(description = "Whether to include events that have already ended. It only affects tasks.", defaultValue = "true")
    private boolean showEnded = true;
    @Schema(description = "Include only events of this types. If empty, all types are included. When filtering task types both deadlines and estimates are included even if only one of this types is specified.", defaultValue = "[]")
    private Optional<List<Integer>> type = Optional.empty();

    public boolean getShowEnded() {
        return showEnded;
    }

    public void setShowEnded(boolean showEnded) {
        this.showEnded = showEnded;
    }

    public Optional<List<Integer>> getType() {
        return type;
    }

    public void setType(List<Integer> type) {
        this.type = Optional.of(type);
    }

    public boolean showTasks() {
        return !type.isPresent() || type.get().contains(EventType.TASK_DEADLINE.ordinal())
                || type.get().contains(EventType.TASK_ESTIMATE.ordinal());
    }

    public boolean showSprints() {
        return !type.isPresent() || type.get().contains(EventType.SPRINT.ordinal());
    }

    public boolean showMeetings() {
        return !type.isPresent() || type.get().contains(EventType.MEETING.ordinal());
    }
}
