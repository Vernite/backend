package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Date;
import java.util.List;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.task.TaskRequest;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class TaskControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private StatusRepository statusRepository;

    private User user;
    private UserSession session;
    private Project project;
    private Project forbiddenProject;
    private Status[] statuses = new Status[2];
    private Status[] forbiddenStatuses = new Status[2];

    void taskEquals(Task expected, Task actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getActive(), actual.getActive());
        assertEquals(expected.getCreatedAt(), actual.getCreatedAt());
        assertEquals(expected.getDeadline(), actual.getDeadline());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getEstimatedDate(), actual.getEstimatedDate());
        assertEquals(expected.getId(), actual.getId());
        assertEquals(expected.getIssue(), actual.getIssue());
    }

    @BeforeAll
    void init() {
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1")));
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_tasks_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }
        project = projectRepository.save(new Project("Tasks project"));
        forbiddenProject = projectRepository.save(new Project("Tasks project forbidden"));
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "tasks test workspace"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        statuses[0] = statusRepository.save(new Status("TO DO", 0, false, true, 0, project));
        statuses[1] = statusRepository.save(new Status("In Progress", 0, false, false, 1, project));
        forbiddenStatuses[0] = statusRepository.save(new Status("TO DO", 0, false, true, 0, forbiddenProject));
        forbiddenStatuses[1] = statusRepository.save(new Status("In Progress", 0, false, false, 1, forbiddenProject));
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
    }

    @Test
    void getAllTasksSuccess() {
        // Test empty return list
        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(0);
        // Prepare some workspaces for next test
        List<Task> tasks = List.of(
                taskRepository.save(new Task("NAME 1", "DESC", statuses[0], user, 0)),
                taskRepository.save(new Task("NAME 3", "DESC", statuses[0], user, 0)),
                taskRepository.save(new Task("NAME 2", "DESC", statuses[0], user, 0)));
        // Test non empty return list
        List<Task> result = client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(3)
                .returnResult()
                .getResponseBody();
        taskEquals(tasks.get(0), result.get(0));
        taskEquals(tasks.get(1), result.get(2));
        taskEquals(tasks.get(2), result.get(1));

        tasks.get(0).setActive(new Date());
        taskRepository.save(tasks.get(0));

        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Task.class)
                .hasSize(2);
    }

    @Test
    void getAllTasksUnauthorized() {
        client.get().uri("/project/{pId}/task", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getAllTasksNotFound() {
        client.get().uri("/project/666/task")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.get().uri("/project/{pId}/task", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void newTaskSuccess() {
        TaskRequest request = new TaskRequest("NAME", "DESC", statuses[0], 0, new Date(), new Date());
        Task parentTask = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        request.setParentTaskId(parentTask.getId());
        Task task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .returnResult()
                .getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));

        request.setCreateIssue(false);
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .returnResult()
                .getResponseBody();
        taskEquals(task, taskRepository.findByIdOrThrow(task.getId()));
    }

    @Test
    void newTaskBadRequest() {
        TaskRequest request = new TaskRequest();
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setName("NAME");
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setDescription("DESC");
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setStatusId(statuses[0].getId());
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setType(1);

        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        Task parentTask = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        parentTask.setParentTask(task);
        parentTask = taskRepository.save(parentTask);

        request.setParentTaskId(parentTask.getId());

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.I_AM_A_TEAPOT);
    }

    @Test
    void newTaskUnauthorized() {
        TaskRequest request = new TaskRequest("NAME", "DESC", statuses[0], 0, new Date(), new Date());
        client.post().uri("/project/{pId}/task", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newTaskNotFound() {
        TaskRequest request = new TaskRequest("NAME", "DESC", forbiddenStatuses[0], 0, new Date(), new Date());
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        request.setStatusId(666L);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        request.setStatusId(statuses[0].getId());
        request.setParentTaskId(666L);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        client.post().uri("/project/666/task")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getTaskSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));

        Task result = client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .returnResult()
                .getResponseBody();
        taskEquals(task, result);
    }

    @Test
    void getTaskUnauthorized() {
        client.get().uri("/project/{pId}/task/1", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));

        client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getTaskNotFound() {
        client.get().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.get().uri("/project/{pId}/task/1", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("NAME", "DESC", forbiddenStatuses[0], user, 0));
        client.get().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void putTaskSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        Task parentTask = taskRepository.save(new Task("NAME 2", "DESC", statuses[0], user, 0));
        TaskRequest request = new TaskRequest();

        Task result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .returnResult()
                .getResponseBody();
        taskEquals(task, result);

        request.setName("NEW PUT");
        task.setName(request.getName().get());

        request.setDescription("NEW PUT");
        task.setDescription(request.getDescription().get());

        request.setDeadline(Date.from(Instant.now().minusSeconds(4000)));
        task.setDeadline(request.getDeadline().get());

        request.setEstimatedDate(Date.from(Instant.now().minusSeconds(4000)));
        task.setEstimatedDate(request.getEstimatedDate().get());

        request.setType(1);
        task.setType(request.getType().get());

        request.setStatusId(statuses[1].getId());
        task.setStatus(statuses[1]);

        request.setParentTaskId(parentTask.getId());
        task.setParentTask(parentTask);

        result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Task.class)
                .returnResult()
                .getResponseBody();
        taskEquals(task, result);
    }

    @Test
    void putTaskBadRequest() {
        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        Task parentTask = taskRepository.save(new Task("NAME 2", "DESC", statuses[0], user, 0));
        Task parentParentTask = taskRepository.save(new Task("NAME 2", "DESC", statuses[0], user, 0));
        parentTask.setParentTask(parentParentTask);
        parentTask = taskRepository.save(parentTask);
        TaskRequest request = new TaskRequest();
        request.setParentTaskId(parentTask.getId());

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.I_AM_A_TEAPOT);
    }

    @Test
    void putTaskUnauthorized() {
        TaskRequest request = new TaskRequest();
        client.put().uri("/project/{pId}/task/1", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void putTaskNotFound() {
        TaskRequest request = new TaskRequest();

        client.put().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        Task forbiddenTask = taskRepository.save(new Task("NAME", "DESC", forbiddenStatuses[0], user, 0));
        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));

        client.put().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), forbiddenTask.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        request.setStatusId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        request.setStatusId(null);
        request.setParentTaskId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteTaskSuccess() {
        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));

        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();
        assertNotEquals(null, taskRepository.findById(task.getId()).get().getActive());
    }

    @Test
    void deleteTaskUnauthorized() {
        client.delete().uri("/project/{pId}/task/1", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("NAME", "DESC", statuses[0], user, 0));
        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();

        assertEquals(task.getActive(), taskRepository.findByIdOrThrow(task.getId()).getActive());
    }

    @Test
    void deleteTaskNotFound() {
        client.delete().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.delete().uri("/project/{pId}/task/1", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("NAME", "DESC", forbiddenStatuses[0], user, 0));
        client.delete().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
