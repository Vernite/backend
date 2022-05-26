package com.workflow.workflow.project;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import javax.validation.constraints.NotNull;

import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.utils.ErrorType;
import com.workflow.workflow.utils.NotFoundRepository;
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
    private StatusRepository statusRepository;

    @Operation(summary = "Create project", description = "Creates new project. Authenticated user is added to project with owner privillages. Project is added to workspace with given id.")
    @ApiResponse(description = "Newly created project.", responseCode = "200")
    @ApiResponse(description = "Some fields are missing or failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping
    public Project newProject(@NotNull @Parameter(hidden = true) User user, @RequestBody ProjectRequest request) {
        if (request.getWorkspaceId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing workspace id field");
        }
        if (request.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name field");
        }
        if (request.getName().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name field length bigger than 50 characters");
        }
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(request.getWorkspaceId(), user));
        Project project = projectRepository.save(new Project(request));
        ProjectWorkspace projectWorkspace = new ProjectWorkspace(project, workspace, 1L);
        projectWorkspaceRepository.save(projectWorkspace);
        statusRepository.save(new Status("TO DO", 0, false, 0, project));
        statusRepository.save(new Status("In Progress", 0, false, 1, project));
        statusRepository.save(new Status("Done", 0, false, 2, project));
        return projectRepository.findById(project.getId()).orElse(project);
    }

    @Operation(summary = "Retrieve project", description = "Retrieves project with given id if authenticated user is member of this project.")
    @ApiResponse(description = "Project with given id.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}")
    public Project getProject(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        return project;
    }

    @Operation(summary = "Modify project", description = "Applies changes from request body to project with given id if authenticated user is member of project. If field from body is missing it wont be changed. Workspace id field is ignored.")
    @ApiResponse(description = "Project after changes.", responseCode = "200")
    @ApiResponse(description = "Some fields failed to satisfy requirements.", responseCode = "400", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}")
    public Project putProject(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @RequestBody ProjectRequest request) {
        if (request.getName() != null && request.getName().length() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name field length bigger than 50 characters");
        }
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        project.apply(request);
        return projectRepository.save(project);
    }

    @Operation(summary = "Delete project", description = "Deletes project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "Project deleted.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/{id}")
    public void deleteProject(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        project.setActive(Date.from(Instant.now().plus(7, ChronoUnit.DAYS)));
        projectRepository.save(project);
    }

    @Operation(summary = "Change project workspace", description = "Changes workspace for project with given id to workspace with given id for authenticated user.")
    @ApiResponse(description = "Project workspace changed", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project or workspace with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PutMapping("/{id}/workspace/{newWorkspaceId}")
    public void moveProjectWorkspace(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable long newWorkspaceId) {
        Project project = projectRepository.findByIdOrThrow(id);
        int index = project.member(user);
        if (index == -1) {
            throw NotFoundRepository.getException();
        }
        Workspace workspace = workspaceRepository.findByIdOrThrow(new WorkspaceKey(newWorkspaceId, user));
        ProjectWorkspace projectWorkspace = project.getProjectWorkspaces().get(index);
        long privillages = projectWorkspace.getPrivileges();
        projectWorkspaceRepository.delete(projectWorkspace);
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, privillages));
    }

    @Operation(summary = "Retrieve project members", description = "Retrieves members of project with given id. Authenticated user must be member of project.")
    @ApiResponse(description = "List of project members.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Project with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/{id}/member")
    public List<ProjectMember> getProjectMembers(@NotNull @Parameter(hidden = true) User user, @PathVariable long id) {
        Project project = projectRepository.findByIdOrThrow(id);
        if (project.member(user) == -1) {
            throw NotFoundRepository.getException();
        }
        return projectWorkspaceRepository.findByProjectOrderByWorkspaceUserUsernameAscWorkspaceUserIdAsc(project)
                .stream().map(ProjectWorkspace::getProjectMember).toList();
    }
}