package com.workflow.workflow;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;

import javax.servlet.http.Cookie;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({"classpath:application.properties", "classpath:application-test.properties"})
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
    @Autowired
    private UserSessionRepository sessionRepository;

    private UserSession session;
    private Workspace workspace;
    private User user;
    private Project project;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email", "Password")));
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_projects_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
                session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
                session = sessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }
        long id = counterSequenceRepository.getIncrementCounter(user.getCounterSequence().getId());
        workspace = workspaceRepository.save(new Workspace(id, user, "name"));
        project = projectRepository.save(new Project("put"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
        statusRepository.deleteAll();
    }

    @Test
    void allEmpty() throws Exception {
        mvc.perform(get(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
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

        mvc.perform(get(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("Name")))
                .andExpect(jsonPath("$[1].name", is("Name")))
                .andExpect(jsonPath("$[2].name", is("Name")));
    }

    @Test
    void allNotFound() throws Exception {
        mvc.perform(get(String.format("/project/%d/status/", 77878)).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .accept(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("string")));
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", 776576)).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.APPLICATION_JSON).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"final\": true}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/status/", project.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                        .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(
                post(String.format("/project/%d/status/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
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

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
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

        mvc.perform(get(String.format("/project/%d/status/%d", 45654645, status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isNotFound());

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), 45334534)).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
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

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.name", is("new put")));

        mvc.perform(get(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
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
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
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
        mvc.perform(put(String.format("/project/%d/status/%d", 231321, status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"string\",\"color\": 0,\"ordinal\": 0,\"final\": true}"))
                .andExpect(status().isNotFound());

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), 2341321)).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
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

        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession()))
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

        mvc.perform(delete(String.format("/project/%d/status/%d", project.getId(), status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete(String.format("/project/%d/status/%d", project.getId(), 32123)).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isNotFound());

        Status status = new Status();
        status.setColor(0);
        status.setFinal(false);
        status.setName("Name");
        status.setOrdinal(0);
        status.setProject(project);
        status = statusRepository.save(status);

        mvc.perform(delete(String.format("/project/%d/status/%d", 2313, status.getId())).cookie(new Cookie(AuthController.COOKIE_NAME, session.getSession())))
                .andExpect(status().isNotFound());
    }
}
