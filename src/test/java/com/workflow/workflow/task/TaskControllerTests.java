package com.workflow.workflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.sprint.Sprint;
import com.workflow.workflow.sprint.SprintRepository;
import com.workflow.workflow.task.time.TimeTrack;
import com.workflow.workflow.task.time.TimeTrackRepository;
import com.workflow.workflow.task.time.TimeTrackRequest;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class TaskControllerTests {
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
    private SprintRepository sprintRepository;
    @Autowired
    private TimeTrackRepository timeTrackRepository;

    private User user;
    private UserSession session;
    private Project project;
    private Sprint sprint;
    private Project forbiddenProject;

    void taskEquals(Task expected, Task actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getActive(), actual.getActive());
        assertEquals(expected.getDeadline(), actual.getDeadline());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getEstimatedDate(), actual.getEstimatedDate());
        assertEquals(expected.getIssue(), actual.getIssue());
        assertEquals(expected.getPriority(), actual.getPriority());
        assertEquals(expected.getPull(), actual.getPull());
    }

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
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
            session = sessionRepository.findBySession("session_token_tasks_tests").orElseThrow();
        }
        project = projectRepository.save(new Project("Tasks project"));
        sprint = sprintRepository.save(new Sprint(1, "name", new Date(), new Date(), "status", "description", project));
        forbiddenProject = projectRepository.save(new Project("Tasks project forbidden"));
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "tasks test workspace"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        taskRepository.deleteAll();
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(0);
        // Prepare some tasks for next test
        List<Task> tasks = List.of(
                new Task(1, "name 1", "description", project.getStatuses().get(0), user, 0, "low"),
                new Task(2, "name 3", "description", project.getStatuses().get(0), user, 0, "low"),
                new Task(3, "name 2", "description", project.getStatuses().get(0), user, 0, "low"));
        taskRepository.saveAll(tasks);
        // Test non empty return list
        List<Task> result = client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(3).returnResult().getResponseBody();
        taskEquals(tasks.get(0), result.get(0));
        taskEquals(tasks.get(1), result.get(2));
        taskEquals(tasks.get(2), result.get(1));
        // Test with soft deleted tasks
        tasks.get(0).setActive(new Date());
        taskRepository.save(tasks.get(0));

        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(2);
    }

    @Test
    void getAllSuccessWithFilter() {
        // Prepare some tasks for next test
        List<Task> tasks = List.of(
                new Task(1, "name 1", "description", project.getStatuses().get(0), user, 0, "low"),
                new Task(2, "name 3", "description", project.getStatuses().get(1), user, 3, "low"),
                new Task(3, "name 2", "description", project.getStatuses().get(2), user, 1, "low"));
        tasks.get(0).setSprint(sprint);
        tasks.get(1).setAssignee(user);
        ArrayList<Task> tasksList = new ArrayList<>();
        taskRepository.saveAll(tasks).forEach(tasksList::add);
        ;
        tasksList.get(2).setParentTask(tasksList.get(1));
        taskRepository.saveAll(tasksList);
        tasks = tasksList;

        client.get().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(3);

        List<Task> result = client.get().uri("/project/{pId}/task?sprintId={sId}", project.getId(), sprint.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1).returnResult().getResponseBody();
        taskEquals(tasks.get(0), result.get(0));

        result = client.get().uri("/project/{pId}/task?assigneeId={iId}", project.getId(), user.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1).returnResult().getResponseBody();
        taskEquals(tasks.get(1), result.get(0));

        result = client.get()
                .uri("/project/{pId}/task?statusId={iId}", project.getId(), project.getStatuses().get(2).getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1).returnResult().getResponseBody();
        taskEquals(tasks.get(2), result.get(0));

        result = client.get()
                .uri("/project/{pId}/task?statusId={iId}&assigneeId={aId}", project.getId(),
                        project.getStatuses().get(1).getNumber(), user.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1).returnResult().getResponseBody();
        taskEquals(tasks.get(1), result.get(0));

        client.get().uri("/project/{pId}/task?type=0", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1);

        client.get().uri("/project/{pId}/task?type=0&type=1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(2);

        client.get().uri("/project/{pId}/task?parentId=1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(0);

        client.get().uri("/project/{pId}/task?parentId=2", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Task.class).hasSize(1);
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{pId}/task", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{pId}/task", -1).cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        client.get().uri("/project/{pId}/task", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        TaskRequest request = new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, "low");
        Task parentTask = taskRepository
                .save(new Task(10, "parent", "desc", project.getStatuses().get(0), user, 0, "low"));
        Task task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByProjectAndNumberOrThrow(project, task.getNumber()));
        taskEquals(task, request.createEntity(1, project.getStatuses().get(0), user));

        request.setParentTaskId(parentTask.getNumber());
        request.setType(Task.TaskType.SUBTASK.ordinal());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByProjectAndNumberOrThrow(project, task.getNumber()));

        request.setSprintId(sprint.getNumber());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByProjectAndNumberOrThrow(project, task.getNumber()));

        request.setAssigneeId(user.getId());
        task = client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, taskRepository.findByProjectAndNumberOrThrow(project, task.getNumber()));
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest(null, "desc", project.getStatuses().get(0).getNumber(), 0, "low")).exchange()
                .expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest(" ", "desc", project.getStatuses().get(0).getNumber(), 0, "low")).exchange()
                .expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("a".repeat(51), "desc", project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", null, project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", null, 0, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), null, "low"))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, null))
                .exchange().expectStatus().isBadRequest();

        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task parentTask = new Task(10, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low");
        parentTask.setParentTask(task);
        parentTask = taskRepository.save(parentTask);

        TaskRequest request = new TaskRequest("name", "desc", project.getStatuses().get(0).getNumber(), 0, "low");
        request.setParentTaskId(parentTask.getNumber());

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();

        request.setParentTaskId(null);
        request.setType(Task.TaskType.SUBTASK.ordinal());

        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{pId}/task", project.getId())
                .bodyValue(new TaskRequest("NAME", "DESC", project.getStatuses().get(0).getNumber(), 0, "low"))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        TaskRequest request = new TaskRequest("n", "d", forbiddenProject.getStatuses().get(0).getNumber(), 0, "low");

        client.post().uri("/project/{pId}/task", forbiddenProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setStatusId(project.getStatuses().get(0).getId());
        request.setParentTaskId(666L);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setParentTaskId(null);
        client.post().uri("/project/{pId}/task", -1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request).exchange().expectStatus().isNotFound();

        request.setSprintId(sprint.getNumber() + 1);
        client.post().uri("/project/{pId}/task", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
    }

    @Test
    void getSuccess() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        Task result = client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);
    }

    @Test
    void getUnauthorized() {
        client.get().uri("/project/{pId}/task/1", project.getId()).exchange().expectStatus().isUnauthorized();
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.get().uri("/project/{pId}/task/{id}", project.getId(), task.getId()).exchange().expectStatus()
                .isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Task task = taskRepository.save(new Task(1, "n", "d", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.get().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task parentTask = taskRepository.save(new Task(2, "NAME 2", "D", project.getStatuses().get(0), user, 0, "low"));

        Task result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TaskRequest()).exchange()
                .expectStatus().isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);

        TaskRequest request = new TaskRequest("new", "new", project.getStatuses().get(1).getNumber(), 1, "medium");
        task.setType(1);
        task.setPriority("medium");
        task.setName(request.getName().get());
        task.setDescription(request.getDescription().get());
        task.setStatus(project.getStatuses().get(1));

        result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);

        request.setDeadline(Date.from(Instant.now().minusSeconds(4000)));
        task.setDeadline(request.getDeadline().get());

        request.setEstimatedDate(Date.from(Instant.now().minusSeconds(4000)));
        task.setEstimatedDate(request.getEstimatedDate().get());

        request.setParentTaskId(parentTask.getNumber());
        task.setParentTask(parentTask);

        request.setType(Task.TaskType.SUBTASK.ordinal());
        task.setType(Task.TaskType.SUBTASK.ordinal());

        request.setSprintId(sprint.getNumber());
        task.setSprint(sprint);

        request.setAssigneeId(user.getId());
        task.setAssignee(user);

        result = client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class).returnResult().getResponseBody();
        taskEquals(task, result);
        assertNotNull(taskRepository.findByIdOrThrow(task.getId()).getSprint());
        assertNotNull(taskRepository.findByIdOrThrow(task.getId()).getAssignee());

        request.setSprintId(null);
        task.setSprint(null);

        request.setAssigneeId(null);
        task.setAssignee(null);

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk().expectBody(Task.class);
        assertNull(taskRepository.findByIdOrThrow(task.getId()).getSprint());
        assertNull(taskRepository.findByIdOrThrow(task.getId()).getAssignee());
    }

    @Test
    void updateBadRequest() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 1, "low"));
        Task parentTask = new Task(2, "NAME 2", "DESC", project.getStatuses().get(0), user, 1, "low");
        Task parentParentTask = taskRepository.save(new Task(3, "2", "D", project.getStatuses().get(0), user, 3, "l"));
        parentTask.setParentTask(parentParentTask);
        parentTask = taskRepository.save(parentTask);
        TaskRequest request = new TaskRequest();
        request.setParentTaskId(parentTask.getNumber());

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();

        request.setParentTaskId(parentParentTask.getNumber());
        request.setType(Task.TaskType.SUBTASK.ordinal());

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();

        request = new TaskRequest();
        request.setType(Task.TaskType.SUBTASK.ordinal());

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        TaskRequest request = new TaskRequest();
        client.put().uri("/project/{pId}/task/1", project.getId()).bodyValue(request).exchange().expectStatus()
                .isUnauthorized();

        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber()).bodyValue(request).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        TaskRequest request = new TaskRequest();

        client.put().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        Task fTask = taskRepository.save(new Task(1, "N", "D", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.put().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), fTask.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setStatusId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request = new TaskRequest();
        request.setParentTaskId(666L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        request.setParentTaskId(null);
        request.setSprintId(2L);
        client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
        // This should return error, but it doesn't.
        // request.setSprintId(null);
        // request.setAssigneeId(10891L);
        // client.put().uri("/project/{pId}/task/{id}", project.getId(), task.getId())
        // .cookie(AuthController.COOKIE_NAME,
        // session.getSession()).bodyValue(request).exchange().expectStatus()
        // .isNotFound();
    }

    @Test
    void deleteSuccess() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotNull(taskRepository.findById(task.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        client.delete().uri("/project/{pId}/task/1", project.getId()).exchange().expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}", project.getId(), task.getNumber()).exchange().expectStatus()
                .isUnauthorized();

        assertEquals(task.getActive(), taskRepository.findByIdOrThrow(task.getId()).getActive());
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/{pId}/task/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Task task = taskRepository.save(new Task(1, "N", "D", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void startTrackingSuccess() throws InterruptedException {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        TimeTrack track = client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(TimeTrack.class).returnResult().getResponseBody();
        assertNull(track.getEndDate());
        assertEquals(false, track.getEdited());
        assertNotNull(track.getStartDate());
    }

    @Test
    void startTrackingUnauthorized() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber()).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void startTrackingNotFound() {
        client.post().uri("/project/{pId}/task/1/track/start", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        Task task = taskRepository
                .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.post().uri("/project/{pId}/task/{id}/track/start", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void startTrackingConflict() throws InterruptedException {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(TimeTrack.class);

        client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void stopTrackingSuccess() throws InterruptedException {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        timeTrackRepository.save(new TimeTrack(user, task));

        TimeTrack track = client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(TimeTrack.class).returnResult().getResponseBody();
        assertNotNull(track.getEndDate());
        assertEquals(false, track.getEdited());
        assertNotNull(track.getStartDate());
    }

    @Test
    void stopTrackingUnauthorized() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber()).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void stopTrackingNotFound() {
        client.post().uri("/project/{pId}/task/1/track/stop", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        Task task = taskRepository
                .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.post().uri("/project/{pId}/task/{id}/track/stop", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void stopTrackingConflict() throws InterruptedException {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));

        client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus()
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void editTrackingSuccess() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        TimeTrack timeTrack = new TimeTrack(user, task);
        timeTrack.setEndDate(new Date());
        timeTrack = timeTrackRepository.save(timeTrack);

        Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

        TimeTrack track = client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest(date, null))
                .exchange().expectStatus().isOk().expectBody(TimeTrack.class).returnResult().getResponseBody();
        assertEquals(true, track.getEdited());
        assertEquals(date, track.getStartDate());

        date = new Date();

        track = client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest(null, date))
                .exchange().expectStatus().isOk().expectBody(TimeTrack.class).returnResult().getResponseBody();

        assertEquals(true, track.getEdited());
        assertEquals(date, track.getEndDate());
    }

    @Test
    void editTrackingBadRequest() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        TimeTrack timeTrack = new TimeTrack(user, task);
        timeTrack = timeTrackRepository.save(timeTrack);

        client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest())
                .exchange().expectStatus().isBadRequest();

        timeTrack.setEndDate(new Date());
        timeTrackRepository.save(timeTrack);
        Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));

        client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest(null, date))
                .exchange().expectStatus().isBadRequest();

        date = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));

        client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest(null, date))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void editTrackingUnauthorized() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.put().uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), 1)
                .bodyValue(new TimeTrackRequest()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void editTrackingNotFound() {
        client.put().uri("/project/{pId}/task/1/track/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest())
                .exchange().expectStatus().isNotFound();
        Task task = taskRepository
                .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.put().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest())
                .exchange().expectStatus().isNotFound();

        task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.put().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest())
                .exchange().expectStatus().isNotFound();

        task = taskRepository
                .save(new Task(3, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        Task task2 = taskRepository.save(new Task(2, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task2));

        client.put()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new TimeTrackRequest(null, null))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteTrackingSuccess() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task));
        client.delete()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
    }

    @Test
    void deleteTrackingUnauthorized() {
        Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}/track/1", project.getId(), task.getNumber())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteTrackingNotFound() {
        client.delete().uri("/project/{pId}/task/1/track/1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        Task task = taskRepository
                .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        client.delete().uri("/project/{pId}/task/{id}/track/1", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Task task2 = taskRepository.save(new Task(2, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
        TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task2));
        client.delete()
                .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();
    }
}
