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

package com.workflow.workflow.project;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.integration.git.GitTaskService;
import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.task.time.TimeTrack;
import com.workflow.workflow.task.time.TimeTrackRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.FieldErrorException;
import com.workflow.workflow.utils.ObjectNotFoundException;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceKey;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/project")
public class ProjectController {

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private WorkspaceRepository workspaceRepository;

    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TimeTrackRepository timeTrackRepository;
    
    @Autowired
    GitTaskService service;

    @Operation(summary = "Create project", description = "Creates new project. Authenticated user is added to project with owner privileges. Project is added to workspace with given id.")
    @ApiResponse(description = "Newly created project.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Project create(@NotNull @Parameter(hidden = true) User user, @RequestBody ProjectRequest request) {
        long id = request.getWorkspaceId().orElseThrow(() -> new FieldErrorException("workspaceId", "missing"));
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(id, user));
        Project project = projectRepository.save(request.createEntity());
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        return project;
    }

    @Operation(summary = "Retrieve project", description = "Retrieves project with given id if authenticated user is member of this project.")
    @ApiResponse(description = "Project with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Project get(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return project;
    }

    @Operation(summary = "Modify project", description = "Applies changes from request body to project with given id if authenticated user is member of project. If field from body is missing it wont be changed. Workspace id field is ignored.")
    @ApiResponse(description = "Project after changes.", responseCode = "200")
    @ApiResponse(description = "Some fields failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Project update(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody ProjectRequest request) {
        Project project = projectRepository.findByIdOrThrow(id);
        int index = project.member(user);
        if (index == -1) {
            throw new ObjectNotFoundException();
        }
        request.getWorkspaceId().ifPresent(workspaceId -> changeWorkspace(workspaceId, index, project, user));
        project.update(request);
        return projectRepository.save(project);
    }

    private void changeWorkspace(long workspaceId, int member, Project project, User user) {
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(workspaceId, user));
        ProjectWorkspace pw = project.getProjectWorkspaces().remove(member);
        projectWorkspaceRepository.delete(pw);
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, pw.getPrivileges()));
        if (pw.getWorkspace().getId().getId() == 0 && pw.getWorkspace().getProjectWorkspaces().isEmpty()) {
            workspaceRepository.delete(pw.getWorkspace());
        }
    }

    @Operation(summary = "Delete project", description = "Deletes project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "Project deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void delete(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        project.softDelete();
        projectRepository.save(project);
    }

    @Operation(summary = "Retrieve project members", description = "Retrieves members of project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "List of project members.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}/member")
    public List<ProjectMember> getProjectMembers(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return projectWorkspaceRepository.findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(project)
                .stream().map(ProjectWorkspace::getProjectMember).toList();
    }

    @Operation(summary = "Add members to projects", description = "Adds members with given emails or usernames to projects with given ids. Authenticated user must be member of projects. If authenticated user is not member of project invited users will not be added to this project but will be added to correct projects (no error will be returned). If user with given email or username is already member of project, nothing will happen. If user with given email or username does not exists nothing will happen.")
    @ApiResponse(description = "List with actual user usernames and list of actual projects.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/member")
    public ProjectInvite addProjectMembers(@NotNull @Parameter(hidden = true) User user,
            @RequestBody ProjectInvite invite) {
        List<User> users = userRepository.findByEmailInOrUsernameIn(invite.getEmails(), invite.getEmails());
        Iterable<Project> projects = projectRepository.findAllById(invite.getProjects());
        List<Project> result = new ArrayList<>();
        for (Project project : projects) {
            if (project.member(user) != -1) {
                result.add(project);
                for (User invitedUser : users) {
                    if (project.member(invitedUser) != -1) {
                        continue;
                    }
                    Workspace workspace = workspaceRepository.findById(new WorkspaceKey(0, invitedUser))
                            .orElseGet(() -> workspaceRepository.save(new Workspace(0, invitedUser, "inbox")));
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

    @Operation(summary = "Remove members from projects", description = "Removes members with given ids from project with given id. Authenticated user must be member of projects and have privilege.")
    @ApiResponse(description = "List with actual users removed from project.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Not enough privileges.", responseCode = "403", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}/member")
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

    @Operation(summary = "Leave project", description = "Authorized user leaves project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "Project member left.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
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

    @Operation(summary = "List time tracking", description = "Retrieves time tracking of project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "List of time tracking.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}/track")
    public List<TimeTrack> getTimeTracks(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return timeTrackRepository.findByTaskStatusProject(project);
    }

    @Operation(summary = "Retrieve git issues for project", description = "Retrieves all issues from all integrated git services for project.")
    @ApiResponse(description = "List of issues.", responseCode = "200", content = @Content(schema = @Schema(implementation = Issue.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}/integration/git/issue")
    public Flux<Issue> getIssues(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return service.getIssues(project);
    }

    @Operation(summary = "Retrieve git pull requests for project", description = "Retrieves all pull requests from all integrated git services for project.")
    @ApiResponse(description = "List of pull requests.", responseCode = "200", content = @Content(schema = @Schema(implementation = PullRequest.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}/integration/git/pull")
    public Flux<PullRequest> getPullRequests(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw new ObjectNotFoundException();
        }
        return service.getPullRequests(project);
    }
}