package com.workflow.workflow.task.time;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.user.User;

public interface TimeTrackRepository extends CrudRepository<TimeTrack, TimeTrackKey> {
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
}
