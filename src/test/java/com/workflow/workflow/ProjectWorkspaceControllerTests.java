package com.workflow.workflow;

import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.WorkspaceRepository;
import com.workflow.workflow.workspace.entity.Workspace;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    private User user;
    private Workspace workspace;
    private Project project;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L)
                .orElseGet(() -> {
                        CounterSequence cs = new CounterSequence();
                        cs = counterSequenceRepository.save(cs);
                        return userRepository.save(new User("Name", "Surname", "Username", "Email", "Password", cs));
                });
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        workspace = workspaceRepository.save(new Workspace(id, user, "name"));
        CounterSequence cs1 = new CounterSequence();
        cs1 = counterSequenceRepository.save(cs1);
        CounterSequence cs2 = new CounterSequence();
        cs2 = counterSequenceRepository.save(cs2);
        CounterSequence cs3 = new CounterSequence();
        cs3 = counterSequenceRepository.save(cs3);
        project = projectRepository.save(new Project("put", cs1, cs2, cs3));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @Test
    void moveWorkspaceNotFound() throws Exception {
        mvc.perform(put(String.format("/project/%d/workspace/%d", project.getId(), 7L)))
                .andExpect(status().isNotFound());
        mvc.perform(put(String.format("/project/%d/workspace/%d", 7L, workspace.getId().getId())))
                .andExpect(status().isNotFound());
                CounterSequence cs1 = new CounterSequence();
                cs1 = counterSequenceRepository.save(cs1);
                CounterSequence cs2 = new CounterSequence();
                cs2 = counterSequenceRepository.save(cs2);
                CounterSequence cs3 = new CounterSequence();
        cs3 = counterSequenceRepository.save(cs3);
                Project newProject = projectRepository.save(new Project("put", cs1, cs2, cs3));
        mvc.perform(put(String.format("/project/%d/workspace/%d", newProject.getId(), workspace.getId().getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void moveWorkspaceSuccess() throws Exception {
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace newWorkspace = workspaceRepository.save(new Workspace(id, user, "test 2"));

        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), workspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(workspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(1)));
        
        mvc.perform(put(String.format("/project/%d/workspace/%d", project.getId(), newWorkspace.getId().getId())))
                .andExpect(status().isOk());
        
        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), workspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(workspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(0)));

        mvc.perform(get(String.format("/user/%d/workspace/%d", user.getId(), newWorkspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is(newWorkspace.getName())))
                .andExpect(jsonPath("$.projectsWithPrivileges", hasSize(1)))
                .andExpect(jsonPath("$.projectsWithPrivileges[0].project.id", is((int) project.getId())));
    }
}
