package com.workflow.workflow.project;

import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceKey;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.workspace.Workspace;
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
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project")
public class ProjectController {
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    static final String USER_NOT_FOUND = "USER_NOT_FOUND";
    static final String PROJECT_NOT_FOUND = "project not found";
    static final String WORKSPACE_NOT_FOUND = "workspace not found";

    @Operation(summary = "Create project.", description = "This method creates new project for user in given workspace. TODO: for now all projects are created for user with id 1, later this will be based on authentication. User creating project is added as member with privileges 1. On success returns newly created project. Throws status 404 when workspace with given id does not exist. Throws status 400 when sent data are incorrect. Throws status 415 when when content type is not application/json.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created project.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Project.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Workspace with given id not found.", content = @Content()),
            @ApiResponse(responseCode = "415", description = "Bad content type.", content = @Content())
    })
    @PostMapping("/")
    public Project add(@RequestBody ProjectRequest request) {
        User user = userRepository.findById(1L) // TODO: for now all projects are created for user with id 1
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(request.getWorkspaceId(), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        WORKSPACE_NOT_FOUND));
        Project project = projectRepository.save(new Project(request));
        ProjectWorkspace projectWorkspace = new ProjectWorkspace(project, workspace, 1L);
        projectWorkspaceRepository.save(projectWorkspace);
        return project;
    }

    @Operation(summary = "Retrive project.", description = "This method is used to retrive project with given id. On success returns project with given id. Throws 404 when project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project with given id.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Project.class))
            }),
            @ApiResponse(responseCode = "404", description = "Project with given id not found.", content = @Content())
    })
    @GetMapping("/{id}")
    public Project get(@PathVariable long id) {
        return projectRepository.findById(id).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
    }

    @Operation(summary = "Modify project.", description = "This method is used to modify existing project. If workspace id is null it is ignored. If workspace id is not null it also changes workspace to given. On success returns modified project. Throws 404 when project or workspace from request data does not exist or user is not a member in project. Throws status 400 when sent data are incorrect. Throws status 415 when when content type is not application/json.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Modified project with given id.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = Workspace.class))
            }),
            @ApiResponse(responseCode = "400", description = "Bad request data format.", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Project with given id not found or user is not a member in project.", content = @Content()),
            @ApiResponse(responseCode = "415", description = "Bad content type.", content = @Content())
    })
    @PutMapping("/{id}")
    public Project put(@PathVariable long id, @RequestBody ProjectRequest request) {
        User user = userRepository.findById(1L) // TODO: user with id 1 again
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        if (request.getWorkspaceId() != null) {
            Workspace workspace = workspaceRepository.findByIdAndUser(request.getWorkspaceId(), user)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            WORKSPACE_NOT_FOUND));
            ProjectWorkspace projectWorkspace = projectWorkspaceRepository
                    .findById(new ProjectWorkspaceKey(workspace, project)).orElseThrow(
                            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not a member in project"));
            projectWorkspace.setWorkspace(workspace);
            projectWorkspaceRepository.save(projectWorkspace);
        }
        project.put(request);
        return projectRepository.save(project);
    }

    @Operation(summary = "Delete project.", description = "This method is used to delete project. On success does not return anything. Throws 404 when project does not exist.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Object with given id has been deleted."),
            @ApiResponse(responseCode = "404", description = "Project with given id not found.")
    })
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        Project project = projectRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        projectWorkspaceRepository.deleteAll(projectWorkspaceRepository.findByProject(project));
        projectRepository.delete(project);
    }
}
