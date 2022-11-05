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

package com.workflow.workflow.meeting;

import java.util.HashSet;
import java.util.List;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.ObjectNotFoundException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("project/{projectId}/meeting")
public class MeetingController {
    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Get all meetings of a project", description = "Get all meetings of a project. The user must be a member of the project.")
    @ApiResponse(description = "The list of meetings.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "The project does not exist.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping
    public List<Meeting> getAll(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return meetingRepository.findAllByProjectAndActiveNull(project);
    }

    @Operation(summary = "Create a meeting", description = "Create a meeting. The user must be a member of the project.")
    @ApiResponse(description = "The meeting.", responseCode = "200")
    @ApiResponse(description = "Some fields are not correct.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "The project does not exist.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Meeting create(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @RequestBody MeetingRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Meeting meeting = request.createEntity(project);
        request.getParticipantIds().ifPresent(participantIds -> {
            HashSet<User> participants = new HashSet<>();
            userRepository.findAllById(participantIds).forEach(participant -> {
                if (project.member(participant) != -1) {
                    participants.add(participant);
                }
            });
            meeting.setParticipants(participants);
        });
        return meetingRepository.save(meeting);
    }

    @Operation(summary = "Get a meeting", description = "Get a meeting. The user must be a member of the project.")
    @ApiResponse(description = "The meeting.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "The project or the meeting does not exist.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{meetingId}")
    public Meeting get(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long meetingId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Meeting meeting = meetingRepository.findByIdOrThrow(meetingId);
        if (meeting.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        return meeting;
    }

    @Operation(summary = "Update a meeting", description = "Update a meeting. The user must be a member of the project.")
    @ApiResponse(description = "The meeting.", responseCode = "200")
    @ApiResponse(description = "Some fields are not correct.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "The project or the meeting does not exist.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{meetingId}")
    public Meeting update(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long meetingId, @RequestBody MeetingRequest request) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Meeting meeting = meetingRepository.findByIdOrThrow(meetingId);
        if (meeting.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        meeting.update(request);
        request.getParticipantIds().ifPresent(participantIds -> {
            HashSet<User> participants = new HashSet<>();
            userRepository.findAllById(participantIds).forEach(participant -> {
                if (project.member(participant) != -1) {
                    participants.add(participant);
                }
            });
            meeting.setParticipants(participants);
        });
        return meetingRepository.save(meeting);
    }

    @Operation(summary = "Delete a meeting", description = "Delete a meeting. The user must be a member of the project.")
    @ApiResponse(description = "The meeting deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "The project or the meeting does not exist.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{meetingId}")
    public Meeting delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long projectId, @PathVariable long meetingId) {
        Project project = projectRepository.findByIdOrThrow(projectId);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        Meeting meeting = meetingRepository.findByIdOrThrow(meetingId);
        if (meeting.getProject().getId() != projectId) {
            throw new ObjectNotFoundException();
        }
        meeting.softDelete();
        return meetingRepository.save(meeting);
    }
}
