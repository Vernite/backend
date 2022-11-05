package com.workflow.workflow.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;

@Service
@Component
public class EventService {
    @Autowired
    private SprintRepository sprintRepository;
    @Autowired
    private TaskRepository taskRepository;

    public List<Event> getUserEvents(User user, Date startDate, Date endDate) {
        TreeSet<Event> result = new TreeSet<>();
        taskRepository.findAllFromUserAndDate(user, startDate, endDate)
                .forEach(task -> result.addAll(Event.from(task)));
        sprintRepository.findAllFromUserAndDate(user, startDate, endDate)
                .forEach(sprint -> result.add(Event.from(sprint)));
        return new ArrayList<>(result);
    }

    public List<Event> getProjectEvents(Project project, Date startDate, Date endDate) {
        TreeSet<Event> result = new TreeSet<>();
        taskRepository.findAllFromProjectAndDate(project, startDate, endDate)
                .forEach(task -> result.addAll(Event.from(task)));
        sprintRepository.findAllFromProjectAndDate(project, startDate, endDate)
                .forEach(sprint -> result.add(Event.from(sprint)));
        return new ArrayList<>(result);
    }
}
