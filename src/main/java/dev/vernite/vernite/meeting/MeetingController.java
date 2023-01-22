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

package dev.vernite.vernite.meeting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;

import io.swagger.v3.oas.annotations.Parameter;

/**
 * Rest controller for performing CRUD operations on Meeting entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("project/{projectId}/meeting")
public class MeetingController {

    private ProjectRepository projectRepository;

    private MeetingRepository meetingRepository;

    private UserRepository userRepository;

    /**
     * Get all meetings of a project. The user must be a member of the project.
     * 
     * @param user      logged in user
     * @param projectId id of the project
     * @return list of meetings sorted by date
     */
    @GetMapping
    public List<Meeting> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return project.getMeetings();
    }

    /**
     * Create a meeting. The user must be a member of the project.
     * 
     * @param user      logged in user
     * @param projectId id of the project
     * @param create    meeting to create
     * @return created meeting
     */
    @PostMapping
    public Meeting create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @RequestBody @Valid CreateMeeting create) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var meeting = new Meeting(project, create);

        if (create.getParticipantIds() != null) {
            meeting.setParticipants(findParticipants(project, create.getParticipantIds()));
        }

        return meetingRepository.save(meeting);
    }

    /**
     * Get a meeting. The user must be a member of the project.
     * 
     * @param user      logged in user
     * @param projectId id of the project
     * @param id        id of the meeting
     * @return the meeting
     */
    @GetMapping("/{id}")
    public Meeting get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        return meetingRepository.findByIdAndProjectOrThrow(id, project);
    }

    /**
     * Update a meeting. The user must be a member of the project.
     * 
     * @param user      logged in user
     * @param projectId id of the project
     * @param id        id of the meeting
     * @param update    data to update
     * @return updated meeting
     */
    @PutMapping("/{id}")
    public Meeting update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id, @RequestBody @Valid UpdateMeeting update) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var meeting = meetingRepository.findByIdAndProjectOrThrow(id, project);

        meeting.update(update);

        if (update.getParticipantIds() != null) {
            meeting.setParticipants(findParticipants(project, update.getParticipantIds()));
        }

        return meetingRepository.save(meeting);
    }

    /**
     * Delete a meeting. The user must be a member of the project.
     * 
     * @param user      logged in user
     * @param projectId id of the project
     * @param id        id of the meeting
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId,
            @PathVariable long id) {
        var project = projectRepository.findByIdAndMemberOrThrow(projectId, user);
        var meeting = meetingRepository.findByIdAndProjectOrThrow(id, project);
        meetingRepository.delete(meeting);
    }

    private Set<User> findParticipants(Project project, List<Long> participantIds) {
        var participants = new HashSet<User>();
        userRepository.findAllById(participantIds).forEach(participant -> {
            if (project.isMember(participant)) {
                participants.add(participant);
            }
        });
        return participants;
    }

}
