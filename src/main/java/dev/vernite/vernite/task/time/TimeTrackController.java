/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2023, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
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

package dev.vernite.vernite.task.time;

import java.util.Date;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

/**
 * Rest controller for performing CRUD operations on TimeTrack entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project/{projectId}/task/{taskId}/track")
public class TimeTrackController {

    private ProjectRepository projectRepository;

    private TaskRepository taskRepository;

    private TimeTrackRepository trackRepository;

    /**
     * Create new time track for given task.
     * 
     * @param user             logged in user
     * @param projectId        project id
     * @param taskId           task id
     * @param timeTrackRequest request body
     * @return created time track
     */
    @PostMapping
    public TimeTrack create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @RequestBody TimeTrackRequest timeTrackRequest) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        TimeTrack timeTrack = timeTrackRequest.createEntity(user, task);
        return trackRepository.save(timeTrack);
    }

    /**
     * Start time tracking for given task. If user is already tracking given task,
     * then this method will return 409 Conflict.
     * 
     * @param user      logged in user
     * @param projectId project id
     * @param taskId    task id
     * @return created time track
     */
    @PostMapping("/start")
    public TimeTrack startTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        if (trackRepository.findByUserAndTaskAndEndDateNull(user, task).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Already tracking");
        }
        return trackRepository.save(new TimeTrack(user, task));
    }

    /**
     * Stop time tracking for given task. If user is not tracking given task, then
     * this method will return 409 Conflict.
     * 
     * @param user      logged in user
     * @param projectId project id
     * @param taskId    task id
     * @return created time track
     */
    @PostMapping("/stop")
    public TimeTrack stopTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        TimeTrack track = trackRepository.findByUserAndTaskAndEndDateNull(user, task)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Not tracking"));
        track.setEndDate(new Date());
        return trackRepository.save(track);
    }

    /**
     * Edit time tracking for given task. Sets edited flag to true.
     * 
     * @param user         logged in user
     * @param projectId    project id
     * @param taskId       task id
     * @param id           time track id
     * @param trackRequest request body
     * @return created time track
     */
    @PutMapping("/{id}")
    public TimeTrack editTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @PathVariable long id, @RequestBody TimeTrackRequest trackRequest) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        TimeTrack timeTrack = trackRepository.findByIdOrThrow(id);
        if (timeTrack.getTask().getId() != task.getId()) {
            throw new ObjectNotFoundException();
        }
        timeTrack.update(trackRequest);
        return trackRepository.save(timeTrack);
    }

    /**
     * Delete time tracking for given task.
     * 
     * @param user      logged in user
     * @param projectId project id
     * @param taskId    task id
     * @param id        time track id
     */
    @DeleteMapping("/{id}")
    public void deleteTracking(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long taskId, @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        Task task = taskRepository.findByProjectAndNumberOrThrow(project, taskId);
        TimeTrack timeTrack = trackRepository.findByIdOrThrow(id);
        if (timeTrack.getTask().getId() != task.getId()) {
            throw new ObjectNotFoundException();
        }
        trackRepository.delete(timeTrack);
    }
}
