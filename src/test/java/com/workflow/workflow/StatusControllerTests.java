package com.workflow.workflow;

import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class StatusControllerTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    private Workspace workspace;
    private User user;
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
        project = projectRepository.save(new Project("put", counterSequenceRepository));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
        statusRepository.deleteAll();
    }

    @Test
    void allEmpty() throws Exception {
        mvc.perform(get(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void allWithData() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        statusRepository.save(status);
        status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        statusRepository.save(status);
        status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        statusRepository.save(status);

        mvc.perform(get(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Name")))
                .andExpect(jsonPath("$[1].name", is("Name")))
                .andExpect(jsonPath("$[2].name", is("Name")));
    }

    @Test
    void allNotFound() throws Exception {
        mvc.perform(get(String.format("/project/%d/status/", 77878)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("string")));
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", 776576)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"final\": true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA)
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA)
                        .content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getSuccess() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), status.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(status.getName())));
    }

    @Test
    void getNotFound() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(get(String.format("/project/%d/status/%d", 45654645, status.getId())))
                .andExpect(status().isNotFound());

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), 45334534)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putSuccess() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), status.getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));
    }

    @Test
    void putBadRequest() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putNotFound() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);
        mvc.perform(put(String.format("/project/%d/status/%d", 231321, status.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isNotFound());

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), 2341321))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putUnsupportedMedia() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void deleteSuccess() throws Exception {
        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(delete(String.format("/project/%d/status/%d", project.getId(), status.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete(String.format("/project/%d/status/%d", project.getId(), 32123)))
                .andExpect(status().isNotFound());

        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(delete(String.format("/project/%d/status/%d", 2313, status.getId())))
                .andExpect(status().isNotFound());
    }
}
