package com.workflow.workflow.status;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
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
class StatusControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private StatusRepository statusRepository;

    private User user;
    private UserSession session;
    private Workspace workspace;
    private Project project;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_status_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_status_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        project = projectRepository.save(new Project("Sprint Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        statusRepository.deleteAll();
    }

    void statusEquals(Status status, Status status2) {
        assertEquals(status.getColor(), status2.getColor());
        assertEquals(status.isBegin(), status2.isBegin());
        assertEquals(status.isFinal(), status2.isFinal());
        assertEquals(status.getName(), status2.getName());
        assertEquals(status.getOrdinal(), status2.getOrdinal());
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Status.class).hasSize(0);
        // Test non-empty return list
        List<Status> statuses = List.of(
                new Status(1, "name 1", 1, false, true, 0, project),
                new Status(2, "name 2", 2, false, false, 1, project),
                new Status(3, "name 3", 3, true, false, 2, project));
        statusRepository.saveAll(statuses);
        List<Status> result = client.get().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Status.class).hasSize(3).returnResult()
                .getResponseBody();
        statusEquals(statuses.get(0), result.get(0));
        statusEquals(statuses.get(0), result.get(0));
        statusEquals(statuses.get(0), result.get(0));
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, "bad_session_token")
                .exchange().expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/status", project.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{projectId}/status", -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.get().uri("/project/{projectId}/status", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        Status result = client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, false, true, 0)).exchange().expectStatus().isOk()
                .expectBody(Status.class).returnResult().getResponseBody();
        Status status = statusRepository.findByProjectAndNumberOrThrow(project, result.getNumber());
        statusEquals(status, result);
    }

    @Test
    void createBadRequest() {
        // Test missing name
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest(null, 1, false, true, 0)).exchange().expectStatus().isBadRequest();
        // Test empty name
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("", 1, false, true, 0)).exchange().expectStatus().isBadRequest();
        // Test too long name
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("a".repeat(51), 1, false, true, 0)).exchange().expectStatus()
                .isBadRequest();
        // Test missing ordinal
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, false, true, null)).exchange().expectStatus()
                .isBadRequest();
        // Test missing color
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", null, false, true, 0)).exchange().expectStatus()
                .isBadRequest();
        // Test missing final
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, null, true, 0)).exchange().expectStatus()
                .isBadRequest();
        // Test missing begin
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, false, null, 0)).exchange().expectStatus()
                .isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{projectId}/status", project.getId())
                .cookie(AuthController.COOKIE_NAME, "bad_session_token")
                .bodyValue(new StatusRequest("name", 1, false, true, 0))
                .exchange().expectStatus().isUnauthorized();

        client.post().uri("/project/{projectId}/status", project.getId())
                .bodyValue(new StatusRequest("name", 1, false, true, 0))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        client.post().uri("/project/{projectId}/status", -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, false, true, 0))
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.post().uri("/project/{projectId}/status", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("name", 1, false, true, 0))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void getSuccess() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        Status result = client.get().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Status.class).returnResult().getResponseBody();
        statusEquals(status, result);
    }

    @Test
    void getUnauthorized() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.get().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getId())
                .cookie(AuthController.COOKIE_NAME, "bad_session_token")
                .exchange().expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{projectId}/status/{statusId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.get().uri("/project/{projectId}/status/{statusId}", project2.getId(), 1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        Status result = client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("new name", 2, true, false, 1)).exchange().expectStatus().isOk()
                .expectBody(Status.class).returnResult().getResponseBody();
        statusEquals(new Status(1, "new name", 2, true, false, 1, project), result);
    }

    @Test
    void updateBadRequest() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest(" ", 2, true, false, 1))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("a".repeat(51), 2, true, false, 1))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, "bad_session_token")
                .bodyValue(new StatusRequest("new name", 2, true, false, 1))
                .exchange().expectStatus().isUnauthorized();

        client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .bodyValue(new StatusRequest("new name", 2, true, false, 1))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.put().uri("/project/{projectId}/status/{statusId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("new name", 2, true, false, 1))
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.put().uri("/project/{projectId}/status/{statusId}", project2.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new StatusRequest("new name", 2, true, false, 1))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk();
        assertNotNull(statusRepository.findById(status.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, "bad_session_token")
                .exchange().expectStatus().isUnauthorized();

        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        Status status2 = statusRepository.save(new Status(1, "name", 1, false, true, 0, project2));
        client.delete().uri("/project/{projectId}/status/{statusId}", project2.getId(), status2.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), status2.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }
}
