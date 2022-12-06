package dev.vernite.vernite.event;

import java.util.List;

import org.springdoc.core.annotations.ParameterObject;

import dev.vernite.vernite.event.Event.Type;

import lombok.Getter;
import lombok.Setter;

/**
 * Parameter object for filtering events.
 */
@ParameterObject
public class EventFilter {

    /**
     * Whether to include events that have already ended. It only affects tasks.
     */
    @Setter
    @Getter
    private boolean showEnded = true;

    /**
     * Types to include in the result. When empty, all types are included. When
     * filtering task types both deadlines and estimates are included even if only
     * one of this types is specified.
     */
    @Setter
    @Getter
    private List<Integer> type = List.of();

    /**
     * Whether to include events that are connected to a tasks.
     * 
     * @return true if task events should be included; false otherwise
     */
    public boolean showTasks() {
        return type.isEmpty() || type.contains(Type.TASK_DEADLINE.ordinal())
                || type.contains(Type.TASK_ESTIMATE.ordinal());
    }

    /**
     * Whether to include events that are connected to a sprints.
     * 
     * @return true if sprint events should be included; false otherwise
     */
    public boolean showSprints() {
        return type.isEmpty() || type.contains(Type.SPRINT.ordinal());
    }

    /**
     * Whether to include events that are connected to a meetings.
     * 
     * @return true if meetings events should be included; false otherwise
     */
    public boolean showMeetings() {
        return type.isEmpty() || type.contains(Type.MEETING.ordinal());
    }

    /**
     * Whether to include events that are connected to a releases.
     * 
     * @return true if release events should be included; false otherwise
     */
    public boolean showReleases() {
        return type.isEmpty() || type.contains(Type.RELEASE.ordinal());
    }
}
