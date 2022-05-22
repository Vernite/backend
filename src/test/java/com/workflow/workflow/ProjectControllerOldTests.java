package com.workflow.workflow;

import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class ProjectControllerOldTests {
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
    private StatusRepository statusRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    private Workspace workspace;
    private User user;

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
    }

    @BeforeEach
    void reset() {
        projectWorkspaceRepository.deleteAll();
        taskRepository.deleteAll();
        statusRepository.deleteAll();
        projectRepository.deleteAll();
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("test")));
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"test\", \"workspaceId\": 700000}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/project/").contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/project/").contentType(MediaType.MULTIPART_FORM_DATA)
                .content(String.format("{\"name\": \"test\", \"workspaceId\": %d}", workspace.getId().getId())))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getMembersSuccess() throws Exception {
        CounterSequence cs1 = new CounterSequence();
        cs1 = counterSequenceRepository.save(cs1);
        CounterSequence cs2 = new CounterSequence();
        cs2 = counterSequenceRepository.save(cs2);
        CounterSequence cs3 = new CounterSequence();
        cs3 = counterSequenceRepository.save(cs3);
        Project project = projectRepository.save(new Project("name 1", cs1, cs2, cs3));

        mvc.perform(get(String.format("/project/%d/member", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        mvc.perform(get(String.format("/project/%d/member", project.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].privileges", is(1)))
                .andExpect(jsonPath("$[0].user.id", is(user.getId().intValue())));
    }

    @Test
    void getMembersNotFound() throws Exception {
        mvc.perform(get("/project/70000/member"))
                .andExpect(status().isNotFound());
    }
}
