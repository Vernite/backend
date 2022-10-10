package com.workflow.workflow.task.time;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.NotFoundRepository;

public interface TimeTrackRepository extends NotFoundRepository<TimeTrack, Long> {
    /**
     * Finds time tracks for a project.
     * 
     * @param project the project.
     * @return list with time tracks.
     */
    List<TimeTrack> findByTaskStatusProject(Project project);

    /**
     * Finds time tracks for a user.
     * 
     * @param user the user.
     * @return list with time tracks.
     */
    List<TimeTrack> findByUser(User user);

    /**
     * Finds time track for a task and user that is currently tracking.
     * 
     * @param user the user.
     * @param task the task.
     * @return the time track. Might be empty.
     */
    Optional<TimeTrack> findByUserAndTaskAndEndDateNull(User user, Task task);
}
