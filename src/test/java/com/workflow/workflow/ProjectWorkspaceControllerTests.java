package com.workflow.workflow;

import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.CoreMatchers.is;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProjectWorkspaceControllerTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private ProjectRepository projectRepository;

    private User user;
    private Workspace workspace;
    private Project project;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L).orElse(userRepository.save(new User("Name", "Surname", "Username", "Email", "Password")));
        workspace = workspaceRepository.save(new Workspace("test", user));
        project = projectRepository.save(new Project("test project"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @Test
    void moveWorkspaceNotFound() throws Exception {
        mvc.perform(patch(String.format("/project/%d/workspace/%d", project.getId(), 7L)))
                .andExpect(status().isNotFound());
        mvc.perform(patch(String.format("/project/%d/workspace/%d", 7L, workspace.getId())))
                .andExpect(status().isNotFound());
        Project newProject = projectRepository.save(new Project("test project 2"));
        mvc.perform(patch(String.format("/project/%d/workspace/%d", newProject.getId(), workspace.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void moveWorkspaceSucess() throws Exception {
        Workspace newWorkspace = workspaceRepository.save(new Workspace("test 2", user));

        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), workspace.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(workspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(1)));
        
        mvc.perform(patch(String.format("/project/%d/workspace/%d", project.getId(), newWorkspace.getId())))
                .andExpect(status().isOk());
        
        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), workspace.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(workspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(0)));

        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), newWorkspace.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(newWorkspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(1)))
                .andExpect(jsonPath("$.projectsWithPrivileges[0].project.id", is(project.getId().intValue())));
    }
}
