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
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource(locations = "classpath:application-test.properties")
public class TaskControllerTests {
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
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;

    private Workspace workspace;
    private Status status;
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
        project = projectRepository.save(new Project("test task"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        Status statusTemp = new Status();
        statusTemp.setColor(0);
        statusTemp.setFinal(false);
        statusTemp.setName("status");
        statusTemp.setOrdinal(0);
        statusTemp.setProject(project);
        status = statusRepository.save(statusTemp);
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
    }

    @Test
    void allEmpty() throws Exception {
        mvc.perform(get(String.format("/project/%d/task/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void allWithData() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        taskRepository.save(task);
        task = new Task();
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task.setName("name 2");
        taskRepository.save(task);
        task = new Task();
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task.setName("name 3");
        taskRepository.save(task);

        mvc.perform(get(String.format("/project/%d/task/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].name", is("name")))
                .andExpect(jsonPath("$[1].name", is("name 2")))
                .andExpect(jsonPath("$[2].name", is("name 3")));
    }

    @Test
    void allNotFound() throws Exception {
        mvc.perform(get(String.format("/project/%d/task/", 77878)).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    void addSuccess() throws Exception {
        mvc.perform(post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .content(String.format("{\"name\": \"string\",\"description\": \"string\",\"statusId\": %d,\"type\": 0,\"deadline\": \"2022-05-11T17:38:27.813Z\",\"createIssue\": false}", status.getId())))
                .andExpect(status().isOk());
        List<Task> tasks = new ArrayList<>();
        tasks.addAll((Collection<? extends Task>) taskRepository.findAll());
        assertEquals(1, tasks.size());
        assertEquals("string", tasks.get(0).getName());
    }

    @Test
    void addNotFound() throws Exception {
        mvc.perform(post(String.format("/project/%d/task/", 776576)).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"name\": \"Patch test\",\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        status.getId())))
                .andExpect(status().isNotFound());

        mvc.perform(post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"name\": \"Patch test\",\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        453543)))
                .andExpect(status().isNotFound());
    }

    @Test
    void addBadRequest() throws Exception {
        mvc.perform(post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/task/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"name\": \"Patch test\",\"description\": \"Patch description\",\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        453543)))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/task/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"name\": \"Patch test\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        453543)))
                .andExpect(status().isBadRequest());
        mvc.perform(post(String.format("/project/%d/task/", project.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        453543)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void addUnsupportedMedia() throws Exception {
        mvc.perform(
                post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA)
                .content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(post(String.format("/project/%d/task/", project.getId())).contentType(MediaType.MULTIPART_FORM_DATA)
                .content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void putSuccess() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);

        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": \"new put\"}"))
                .andExpect(status().isOk());

        assertEquals("new put", taskRepository.findById(task.getId()).orElseThrow().getName());
    }

    @Test
    void putBadRequest() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);
        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void putNotFound() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);
        mvc.perform(put(String.format("/project/%d/task/%d", 231321, task.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        status.getId())))
                .andExpect(status().isNotFound());

        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), 2341321))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        status.getId())))
                .andExpect(status().isNotFound());

        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format(
                        "{\"description\": \"Patch description\",\"type\": 0,\"deadline\": \"2022-05-08T11:56:14.384+00:00\",\"statusId\": %d, \"createIssue\": false}",
                        321321)))
                .andExpect(status().isNotFound());
    }

    @Test
    void putUnsupportedMedia() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);

        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{}"))
                .andExpect(status().isUnsupportedMediaType());
        mvc.perform(put(String.format("/project/%d/task/%d", project.getId(), task.getId()))
                .contentType(MediaType.MULTIPART_FORM_DATA).content("{\"name\": \"test\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void deleteSuccess() throws Exception {
        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);

        mvc.perform(delete(String.format("/project/%d/task/%d", project.getId(), task.getId())))
                .andExpect(status().isOk());
    }

    @Test
    void deleteNotFound() throws Exception {
        mvc.perform(delete(String.format("/project/%d/task/%d", project.getId(), 32123)))
                .andExpect(status().isNotFound());

        Task task = new Task();
        task.setName("name");
        task.setCreatedAt(new Date());
        task.setDeadline(new Date());
        task.setDescription("description");
        task.setType(0);
        task.setUser(user);
        task.setStatus(status);
        task = taskRepository.save(task);

        mvc.perform(delete(String.format("/project/%d/task/%d", 2313, task.getId())))
                .andExpect(status().isNotFound());
    }
}
