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
public class WorkspaceControllerTests {
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

    @BeforeAll
    void init() {
        if (userRepository.findById(1L).isEmpty()) {
                CounterSequence cs = new CounterSequence();
                cs = counterSequenceRepository.save(cs);
                userRepository.save(new User("Name", "Surname", "Username", "Email", "Password", cs));
        }
        projectWorkspaceRepository.deleteAll();
    }

    @BeforeEach
    void reset() {
        workspaceRepository.deleteAll();
    }

    @Test
    void allEmpty() throws Exception {
        mvc.perform(get("/user/1/workspace/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void allWithData() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        workspaceRepository.save(new Workspace(id, user, "test 1"));
        id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        workspaceRepository.save(new Workspace(id, user, "test 2"));
        id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        workspaceRepository.save(new Workspace(id, user, "test 3"));

        mvc.perform(get("/user/1/workspace/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("test 1")))
                .andExpect(jsonPath("$[1].name", is("test 2")))
                .andExpect(jsonPath("$[2].name", is("test 3")));
    }

    @Test
    void allNotFound() throws Exception {
        mvc.perform(get("/user/2222/workspace/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"test\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("test")));
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post("/user/2222/workspace/").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"test\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post("/user/1/workspace/").contentType(MediaType.MULTIPART_FORM_DATA)
                .content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void getSuccess() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(get(String.format("/user/1/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is(workspace.getName())));
    }

    @Test
    void getNotFound() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(get("/user/1/workspace/54"))
                .andExpect(status().isNotFound());

        mvc.perform(get(String.format("/user/2222/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void putSuccess() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(put(String.format("/user/1/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));

        mvc.perform(get(String.format("/user/1/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));
    }

    @Test
    void putBadRequest() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(put(String.format("/user/1/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putNotFound() throws Exception {
        mvc.perform(put("/user/1/workspace/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(put("/user/2222/workspace/1").contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isNotFound());

                User user = userRepository.findById(1L).orElseThrow();
                long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
                Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(put(String.format("/user/2222/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\": \"new put\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void putUnsupportedMedia() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(put(String.format("/user/1/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/user/1/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/user/1/workspace/%d", workspace.getId().getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void deleteSuccess() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(delete(String.format("/user/1/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete("/user/1/workspace/223"))
                .andExpect(status().isNotFound());

                User user = userRepository.findById(1L).orElseThrow();
                long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
                Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));

        mvc.perform(delete(String.format("/user/222/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteBadRequest() throws Exception {
        User user = userRepository.findById(1L).orElseThrow();
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        Workspace workspace = workspaceRepository.save(new Workspace(id, user, "test 176"));
        CounterSequence cs1 = new CounterSequence();
        cs1 = counterSequenceRepository.save(cs1);
        CounterSequence cs2 = new CounterSequence();
        cs2 = counterSequenceRepository.save(cs2);
        CounterSequence cs3 = new CounterSequence();
        cs3 = counterSequenceRepository.save(cs3);
        Project project = projectRepository.save(new Project("put", cs1, cs2, cs3));
        ProjectWorkspace projectWorkspace = projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        mvc.perform(delete(String.format("/user/1/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isBadRequest());
        
        projectWorkspaceRepository.delete(projectWorkspace);

        mvc.perform(delete(String.format("/user/1/workspace/%d", workspace.getId().getId())))
                .andExpect(status().isOk());
        
        projectRepository.delete(project);
    }
}
