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

package dev.vernite.vernite.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import dev.vernite.vernite.auditlog.AuditLog;
import dev.vernite.vernite.auditlog.AuditLogRepository;
import dev.vernite.vernite.cdn.File;
import dev.vernite.vernite.cdn.FileManager;
import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.event.EventService;
import dev.vernite.vernite.integration.calendar.CalendarIntegration;
import dev.vernite.vernite.integration.calendar.CalendarIntegrationRepository;
import dev.vernite.vernite.integration.git.Branch;
import dev.vernite.vernite.integration.git.GitTaskService;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.projectworkspace.ProjectMember;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.task.time.TimeTrack;
import dev.vernite.vernite.task.time.TimeTrackRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ImageConverter;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import dev.vernite.vernite.utils.SecureStringUtils;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceId;
import dev.vernite.vernite.workspace.WorkspaceRepository;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import reactor.core.publisher.Flux;

/**
 * Rest controller for performing CRUD operations on Projects entities.
 */
@RestController
@AllArgsConstructor
@RequestMapping("/project")
public class ProjectController {

    private ProjectRepository projectRepository;

    private WorkspaceRepository workspaceRepository;

    private ProjectWorkspaceRepository projectWorkspaceRepository;

    private UserRepository userRepository;

    private TimeTrackRepository timeTrackRepository;

    private EventService eventService;

    private FileManager fileManager;

    private CalendarIntegrationRepository calendarRepository;

    private AuditLogRepository auditLogRepository;

    private GitTaskService service;

    /**
     * Create new project. Creating user will be automatically added to that
     * project.
     * 
     * @param user   logged in user
     * @param create data for new project
     * @return newly created project
     */
    @PostMapping
    public Project create(@NotNull @Parameter(hidden = true) User user, @RequestBody @Valid CreateProject create) {
        long id = create.getWorkspaceId();
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceId(id, user.getId()));
        Project project = projectRepository.save(new Project(create));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        return project;
    }

    /**
     * Retrieve project. If user is not member of project with given ID this method
     * returns not found error.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return project with given ID
     */
    @GetMapping("/{id}")
    public Project get(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        return projectRepository.findByIdAndMemberOrThrow(id, user);
    }

    /**
     * Update project with given ID. Performs partial update using only supplied
     * fields from request body. Authenticated user must be member of project.
     * 
     * @param user   logged in user
     * @param id     ID of project
     * @param update data to update
     * @return project after update
     */
    @PutMapping("/{id}")
    public Project update(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody @Valid UpdateProject update) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        if (update.getWorkspaceId() != null) {
            changeWorkspace(update.getWorkspaceId(), project, user);
        }
        project.update(update);
        return projectRepository.save(project);
    }

    private void changeWorkspace(long workspaceId, Project project, User user) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceId(workspaceId, user.getId()));
        ProjectWorkspace pw = project.removeMember(user);
        projectWorkspaceRepository.delete(pw);
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, pw.getPrivileges()));
        if (pw.getWorkspace().getId().getId() == 0 && pw.getWorkspace().getProjectWorkspaces().isEmpty()) {
            workspaceRepository.delete(pw.getWorkspace());
        }
    }

    /**
     * Delete project with given ID. Project will be soft deleted and full delete
     * wil happen after a week. Authenticated user must be member of project.
     * 
     * @param user logged in user
     * @param id   ID of project
     */
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        project.softDelete();
        projectRepository.save(project);
    }

    /**
     * Retrieve project members. Authenticated user must be member of project.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return list of project members
     */
    @GetMapping("/{id}/member")
    public List<ProjectMember> getProjectMembers(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return projectWorkspaceRepository.findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(project)
                .stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    /**
     * Retrieve project member. Authenticated user must be member of project.
     * 
     * @param user     logged in user
     * @param id       ID of project
     * @param memberId ID of searched user
     * @return project member
     */
    @GetMapping("/{id}/member/{memberId}")
    public ProjectMember getProjectMember(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long memberId) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return projectWorkspaceRepository.findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(project)
                .stream().map(ProjectWorkspace::getProjectMember).filter(member -> member.user().getId() == memberId)
                .findFirst().orElseThrow(ObjectNotFoundException::new);
    }

    /**
     * Adds members to projects. Adds every given user to every given project. In
     * order to add users to project authenticated user must be member of every
     * project. If authenticated user is not member of project no one will be added
     * to this project. If user with given email / username does not exists no error
     * will be thrown.
     * 
     * @param user   logged in user
     * @param invite list of project and user to add.
     * @return list with project and users which were added to projects
     */
    @PostMapping("/member")
    public ProjectInvite addProjectMembers(@NotNull @Parameter(hidden = true) User user,
            @RequestBody ProjectInvite invite) {
        List<User> users = userRepository.findByEmailInOrUsernameIn(invite.getEmails(), invite.getEmails());
        Iterable<Project> projects = projectRepository.findAllById(invite.getProjects());
        List<Project> result = new ArrayList<>();
        for (Project project : projects) {
            if (project.isMember(user)) {
                result.add(project);
                for (User invitedUser : users) {
                    if (project.isMember(invitedUser)) {
                        continue;
                    }
                    Workspace workspace = workspaceRepository.findById(new WorkspaceId(0, invitedUser.getId()))
                            .orElseGet(() -> workspaceRepository.save(new Workspace(0, "inbox", invitedUser)));
                    projectWorkspaceRepository
                            .save(new ProjectWorkspace(project, workspace, 2L));
                }
            }
        }
        if (result.isEmpty() || users.isEmpty()) {
            return new ProjectInvite(new ArrayList<>(), new ArrayList<>());
        }
        return new ProjectInvite(users.stream().map(User::getUsername).toList(), result);
    }

    /**
     * Remove members from project. Authenticated user must be member of project and
     * have sufficient privileges.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @param ids  IDs of users to remove
     * @return removed users
     */
    @PutMapping("/{id}/member")
    @ApiResponse(description = "List with actual users removed from project.", responseCode = "200")
    @ApiResponse(description = "Not enough privileges.", responseCode = "403", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    public List<User> deleteMember(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody List<Long> ids) {
        Project project = projectRepository.findByIdOrThrow(id);
        int index = project.member(user);
        if (index == -1) {
            throw new ObjectNotFoundException();
        }
        if (project.getProjectWorkspaces().get(index).getPrivileges() != 1L) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Iterable<User> users = userRepository.findAllById(ids.stream().filter(i -> i != user.getId()).toList());
        List<ProjectWorkspace> projectWorkspaces = projectWorkspaceRepository.findByWorkspaceUserInAndProject(users,
                project);
        projectWorkspaceRepository.deleteAll(projectWorkspaces);
        return projectWorkspaces.stream().map(ps -> ps.getWorkspace().getUser()).toList();
    }

    /**
     * Leave project. Authenticated user leaves project with given ID.
     * 
     * @param user logged in user
     * @param id   ID of project
     */
    @DeleteMapping("/{id}/member")
    public void leaveProject(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        int index = project.member(user);
        if (index == -1) {
            throw new ObjectNotFoundException();
        }
        ProjectWorkspace pw = project.getProjectWorkspaces().get(index);
        projectWorkspaceRepository.delete(pw);
    }

    /**
     * Retrieve project time tracks. Authenticated user must be member of project.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return list with all time tracks from given project
     */
    @GetMapping("/{id}/track")
    public List<TimeTrack> getTimeTracks(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return timeTrackRepository.findByTaskStatusProject(project);
    }

    /**
     * Retrieve git issues for project. Retrieve all issues from integrated git
     * providers.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return list with issues
     */
    @GetMapping("/{id}/integration/git/issue")
    public Flux<Issue> getIssues(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return service.getIssues(project);
    }

    /**
     * Retrieve git pull requests for project. Retrieve all pull requests from
     * integrated git providers.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return list with pull requests
     */
    @GetMapping("/{id}/integration/git/pull")
    public Flux<PullRequest> getPullRequests(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return service.getPullRequests(project);
    }

    /**
     * Retrieve git branches for project. Retrieve all branches from integrated git
     * providers.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return list with branches
     */
    @GetMapping("/{id}/integration/git/branch")
    public Flux<Branch> getBranches(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return service.getBranches(project);
    }

    /**
     * Retrieve events for project.
     * 
     * @param user   logged in user
     * @param id     ID of project
     * @param from   timestamp after events happen
     * @param to     timestamp before events happen
     * @param filter filter for events
     * @return list with events after 'from' and before 'to' filtered by 'filter'
     */
    @GetMapping("/{id}/events")
    public Set<Event> getEvents(@NotNull @Parameter(hidden = true) User user, @PathVariable long id, long from,
            long to, @ModelAttribute EventFilter filter) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return eventService.getProjectEvents(project, new Date(from), new Date(to), filter);
    }

    /**
     * Update project logo. Given file will be converted to image/webp format with
     * resolution 400x400. Alpha channel is supported.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @param file new logo image
     * @return new logo file information
     */
    @PostMapping(path = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ApiResponse(description = "Project logo changed.", responseCode = "200")
    @ApiResponse(description = "Cannot convert image.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    public File uploadLogo(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestParam("file") MultipartFile file) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        byte[] converted;
        try {
            converted = ImageConverter.convertImage(file.getOriginalFilename(), file.getBytes());
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        File f = fileManager.uploadFile("image/webp", converted);
        project.setLogo(f);
        project = projectRepository.save(project);
        return f;
    }

    /**
     * Delete project logo. After that logo will be empty.
     * 
     * @param user logged in user
     * @param id   ID of project
     */
    @DeleteMapping(path = "/{id}/logo")
    public void uploadImage(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        project.setLogo(null);
        project = projectRepository.save(project);
    }

    /**
     * Create calendar synchronization link. Creates link for iCalendar format
     * synchronization of project calendar.
     * 
     * @param user logged in user
     * @param id   ID of project
     * @return link to project calendar in iCalendar format
     */
    @PostMapping("/{id}/events/sync")
    public String createCalendarSync(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        String key = SecureStringUtils.generateRandomSecureString();
        while (calendarRepository.findByKey(key).isPresent()) {
            key = SecureStringUtils.generateRandomSecureString();
        }
        Optional<CalendarIntegration> integration = calendarRepository.findByUserAndProject(user, project);
        if (integration.isPresent()) {
            key = integration.get().getKey();
        } else {
            calendarRepository.save(new CalendarIntegration(user, project, key));
        }
        return "https://vernite.dev/api/webhook/calendar?key=" + key;
    }

    @GetMapping("/{id}/auditlog")
    public List<AuditLog> getAuditLog(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdAndMemberOrThrow(id, user);
        return auditLogRepository.findByProject(project);
    }

}