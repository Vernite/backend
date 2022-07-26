package com.workflow.workflow.sprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
public class SprintControllerTests {

    @Autowired
    private WebTestClient client;
    @Autowired
    private SprintRepository sprintRepository;
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
        session.setSession("session_token_sprint_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_sprint_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        project = projectRepository.save(new Project("Sprint Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void reset() {
        sprintRepository.deleteAll();
    }

    @Test
    void getAllSprintsSuccess() {
        // Test empty return list
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Sprint.class).hasSize(0);
        // Test non-empty return list
        List<Sprint> sprints = List.of(
                sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "open", "desc", project)),
                sprintRepository.save(new Sprint("Sprint 2", new Date(), new Date(), "open", "desc", project)),
                sprintRepository.save(new Sprint("Sprint 3", new Date(), new Date(), "open", "desc", project)));
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Sprint.class).hasSize(3).isEqualTo(sprints);
    }

    @Test
    void getAllSprintsUnauthorized() {
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllSprintsNotFound() {
        client.get().uri("/project/{projectId}/sprint", -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.get().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void newSprintSuccess() {
        SprintRequest request = new SprintRequest("Sprint 1", "desc", new Date(), new Date(), "open");
        Sprint sprint = client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isOk().expectBody(Sprint.class).returnResult().getResponseBody();
        assertEquals(request.getName(), sprint.getName());
        assertEquals(request.getDescription(), sprint.getDescription());
        assertEquals(request.getFinishDate(), sprint.getFinishDate());
        assertEquals(request.getStartDate(), sprint.getStartDate());
        assertEquals(request.getStatus(), sprint.getStatus());
    }

    @Test
    void newSprintUnauthorized() {
        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        SprintRequest request = new SprintRequest("Sprint 1", "desc", new Date(), new Date(), "open");
        client.post().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void newSprintBadRequest() {
        SprintRequest request = new SprintRequest();
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setName("");
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setName("a".repeat(51));
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setName("Sprint 1");
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setStartDate(new Date());
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setFinishDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setFinishDate(new Date());
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setStatus("");
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void newSprintNotFound() {
        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        SprintRequest request = new SprintRequest("Sprint 1", "desc", new Date(), new Date(), "open");
        client.post().uri("/project/{projectId}/sprint", -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isNotFound();
        client.post().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void getSprintSuccess() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBody(Sprint.class).isEqualTo(sprint);
    }

    @Test
    void getSprintUnauthorized() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getSprintNotFound() {
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("Sprint 2", new Date(), new Date(), "desc", "open", project2));
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void putSprintSuccess() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        SprintRequest request = new SprintRequest("Sprint 2", "desc", new Date(), new Date(), "open");
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isOk();
        Sprint updatedSprint = sprintRepository.findById(sprint.getId()).get();
        assertEquals(request.getName(), updatedSprint.getName());
        assertEquals(request.getDescription(), updatedSprint.getDescription());
        assertEquals(request.getStartDate(), updatedSprint.getStartDate());
        assertEquals(request.getFinishDate(), updatedSprint.getFinishDate());
        assertEquals(request.getStatus(), updatedSprint.getStatus());
    }

    @Test
    void putSprintBadRequest() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        SprintRequest request = new SprintRequest("", "desc", null, null, "open");
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setName("a".repeat(51));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setName("Sprint 1");
        request.setFinishDate(Date.from(Instant.now().minus(1, ChronoUnit.DAYS)));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setFinishDate(null);
        request.setStartDate(Date.from(Instant.now().plus(1, ChronoUnit.DAYS)));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();

        request.setFinishDate(new Date());
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void putSprintUnauthorized() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        SprintRequest request = new SprintRequest("Sprint 2", "desc", new Date(), new Date(), "open");
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void putSprintNotFound() {
        SprintRequest request = new SprintRequest("Sprint 1", "desc", new Date(), new Date(), "open");
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isNotFound();
        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("Sprint 2", new Date(), new Date(), "desc", "open", project2));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isNotFound();

        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request)
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSprintSuccess() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk();
        assertNotNull(sprintRepository.findById(sprint.getId()).get().getActive());
    }

    @Test
    void deleteSprintUnauthorized() {
        Sprint sprint = sprintRepository.save(new Sprint("Sprint 1", new Date(), new Date(), "desc", "open", project));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .exchange().expectStatus().isUnauthorized();
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteSprintNotFound() {
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("Sprint 2", new Date(), new Date(), "desc", "open", project2));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }
}
