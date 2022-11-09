package dev.vernite.vernite.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import dev.vernite.vernite.meeting.MeetingRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.sprint.SprintRepository;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;

@Service
@Component
public class EventService {
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private MeetingRepository meetingRepository;

    /**
     * Returns all events for the given user between dates.
     * 
     * @param user      the user
     * @param startDate the start date
     * @param endDate   the end date
     * @param filter    the filter
     * @return a list of events
     */
    public List<Event> getUserEvents(User user, Date startDate, Date endDate, EventFilter filter) {
        TreeSet<Event> result = new TreeSet<>();
        if (filter.showTasks()) {
            taskRepository.findAllFromUserAndDate(user, startDate, endDate, filter)
                    .forEach(task -> result.addAll(Event.from(task)));
        }
        if (filter.showSprints()) {
            sprintRepository.findAllFromUserAndDate(user, startDate, endDate)
                    .forEach(sprint -> result.add(Event.from(sprint)));
        }
        if (filter.showMeetings()) {
            meetingRepository.findMeetingsByUserAndDate(user, startDate, endDate)
                    .forEach(meeting -> result.add(Event.from(meeting)));
        }
        return new ArrayList<>(result);
    }

    /**
     * Returns all events for the given project between dates.
     * 
     * @param project   the project
     * @param startDate the start date
     * @param endDate   the end date
     * @param filter    the filter
     * @return a list of events
     */
    public List<Event> getProjectEvents(Project project, Date startDate, Date endDate, EventFilter filter) {
        TreeSet<Event> result = new TreeSet<>();
        if (filter.showTasks()) {
            taskRepository.findAllFromProjectAndDate(project, startDate, endDate, filter)
                    .forEach(task -> result.addAll(Event.from(task)));
        }
        if (filter.showSprints()) {
            sprintRepository.findAllFromProjectAndDate(project, startDate, endDate)
                    .forEach(sprint -> result.add(Event.from(sprint)));
        }
        if (filter.showMeetings()) {
            meetingRepository.findMeetingsByProjectAndDate(project, startDate, endDate)
                    .forEach(meeting -> result.add(Event.from(meeting)));
        }
        return new ArrayList<>(result);
    }
}