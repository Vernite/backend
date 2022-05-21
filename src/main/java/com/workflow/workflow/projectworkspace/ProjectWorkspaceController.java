package com.workflow.workflow.projectworkspace;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.workspace.WorkspaceRepository;
import com.workflow.workflow.workspace.entity.Workspace;
import com.workflow.workflow.workspace.entity.WorkspaceKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/project/{projectId}")
public class ProjectWorkspaceController {
    private static final String USER_NOT_FOUND = "user not found";
    private static final String PROJECT_NOT_FOUND = "project not found";
    private static final String WORKSPACE_NOT_FOUND = "workspace not found";
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    @Operation(summary = "Move project to another workspace.", description = "This method is used to move project to another workspace. For now uses user with id 1 later will be based on authentication. On success does not return anything. Throws 404 when project or workspace does not exist. Throws 404 when user is not member of project.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Project has been moved."),
            @ApiResponse(responseCode = "404", description = "Workspace or project with given id not found. User is not member of project")
    })
    @PutMapping("/workspace/{newWorkspaceId}")
    public void moveWorkspace(@PathVariable long projectId, @PathVariable long newWorkspaceId) {
        User user = userRepository.findById(1L) // TODO: user with id 1 again
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, USER_NOT_FOUND));
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, PROJECT_NOT_FOUND));
        Workspace workspace = workspaceRepository.findByIdAndUser(new WorkspaceKey(newWorkspaceId, user), user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, WORKSPACE_NOT_FOUND));
        ProjectWorkspace projectWorkspace = projectWorkspaceRepository.findByProjectAndWorkspaceUser(project, user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not a member of project"));
        projectWorkspaceRepository.delete(projectWorkspace);
        projectWorkspace.setWorkspace(workspace);
        projectWorkspaceRepository.save(projectWorkspace);
    }
}
