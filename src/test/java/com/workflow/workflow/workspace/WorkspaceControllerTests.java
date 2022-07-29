package com.workflow.workflow.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;

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

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class WorkspaceControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    private User user;
    private UserSession session;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_workspace_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_workspace_tests").orElseThrow();
        }
    }

    @BeforeEach
    void clean() {
        workspaceRepository.deleteAll();
    }

    void workspaceEquals(Workspace w1, Workspace w2) {
        assertEquals(w1.getId().getId(), w2.getId().getId());
        assertEquals(w1.getName(), w2.getName());
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isOk().expectBodyList(Workspace.class).hasSize(0);
        // Prepare some workspaces for next test
        List<Workspace> workspaces = List.of(new Workspace(1, user, "Test 1"), new Workspace(2, user, "Test 3"),
                new Workspace(3, user, "Test 2"));
        workspaceRepository.saveAll(workspaces);
        // Test non empty return list
        List<Workspace> result = client.get().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Workspace.class).hasSize(3).returnResult()
                .getResponseBody();
        workspaceEquals(workspaces.get(0), result.get(0));
        workspaceEquals(workspaces.get(1), result.get(2));
        workspaceEquals(workspaces.get(2), result.get(1));
        // Test soft delete
        workspaces.get(0).setActive(new Date());
        workspaceRepository.save(workspaces.get(0));
        client.get().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isOk().expectBodyList(Workspace.class).hasSize(2);
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/workspace").cookie(AuthController.COOKIE_NAME, "invalid_token").exchange().expectStatus()
                .isUnauthorized();
        client.get().uri("/workspace").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createSuccess() {
        Workspace workspace = client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest("POST")).exchange().expectStatus().isOk().expectBody(Workspace.class)
                .returnResult().getResponseBody();
        Optional<Workspace> optional = workspaceRepository.findById(new WorkspaceKey(workspace.getId().getId(), user));
        assertEquals(true, optional.isPresent());
        Workspace result = optional.get();
        workspaceEquals(result, workspace);
    }

    @Test
    void createBadRequest() {
        // Test null name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest(null)).exchange().expectStatus().isBadRequest();
        // Test empty name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest("")).exchange().expectStatus().isBadRequest();
        // Test too long name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest("a".repeat(51))).exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, "invalid_token").exchange().expectStatus()
                .isUnauthorized();
        client.post().uri("/workspace").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "GET"));

        Workspace result = client.get().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Workspace.class).returnResult().getResponseBody();

        workspaceEquals(workspace, result);
    }

    @Test
    void getUnathorized() {
        client.get().uri("/workspace/1").exchange().expectStatus().isUnauthorized();

        long id = workspaceRepository.save(new Workspace(1, user, "GET")).getId().getId();
        client.get().uri("/workspace/{id}", id).exchange().expectStatus().isUnauthorized();
        client.get().uri("/workspace/{id}", id).cookie(AuthController.COOKIE_NAME, "invalid_token").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Workspace workspace = new Workspace(1, user, "GET");
        workspace.setActive(new Date());
        workspace = workspaceRepository.save(workspace);
        client.get().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "PUT"));

        client.put().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new WorkspaceRequest()).exchange()
                .expectStatus().isOk();

        Workspace result = client.put().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new WorkspaceRequest("NEW PUT"))
                .exchange().expectStatus().isOk().expectBody(Workspace.class).returnResult().getResponseBody();
        workspace.setName("NEW PUT");
        workspaceEquals(workspace, result);
        workspaceEquals(workspace, workspaceRepository.findByIdOrThrow(workspace.getId()));
    }

    @Test
    void updateBadRequest() {
        long id = workspaceRepository.save(new Workspace(1, user, "PUT")).getId().getId();

        client.put().uri("/workspace/{id}", id)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new WorkspaceRequest("")).exchange()
                .expectStatus().isBadRequest();

        client.put().uri("/workspace/{id}", id)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest("a".repeat(51))).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateUnathorized() {
        client.put().uri("/workspace/1").bodyValue(new WorkspaceRequest("NEW PUT")).exchange().expectStatus()
                .isUnauthorized();

        long id = workspaceRepository.save(new Workspace(1, user, "PUT")).getId().getId();
        client.put().uri("/workspace/{id}", id).bodyValue(new WorkspaceRequest("NEW PUT")).exchange().expectStatus()
                .isUnauthorized();
        client.put().uri("/workspace/{id}", id).cookie(AuthController.COOKIE_NAME, "invalid_token")
                .bodyValue(new WorkspaceRequest("NEW PUT")).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        client.put().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new WorkspaceRequest("NEW PUT")).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess(@Autowired ProjectRepository pRepo, @Autowired ProjectWorkspaceRepository pwRepo) {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotEquals(null, workspaceRepository.findById(workspace.getId()).get().getActive());

        Project project = new Project("DELETE");
        project.setActive(new Date());
        project = pRepo.save(project);
        Workspace workspace2 = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        pwRepo.save(new ProjectWorkspace(project, workspace2, 1L));

        client.delete().uri("/workspace/{id}", workspace2.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotEquals(null, workspaceRepository.findById(workspace2.getId()).get().getActive());
    }

    @Test
    void deleteBadRequest(@Autowired ProjectRepository pRepo, @Autowired ProjectWorkspaceRepository pwRepo) {
        Project project = pRepo.save(new Project("DELETE"));
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        pwRepo.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isBadRequest();
        assertEquals(workspace.getActive(), workspaceRepository.findByIdOrThrow(workspace.getId()).getActive());
    }

    @Test
    void deleteUnauthorized() {
        client.delete().uri("/workspace/1").exchange().expectStatus().isUnauthorized();

        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        client.delete().uri("/workspace/{id}", workspace.getId().getId()).exchange().expectStatus().isUnauthorized();
        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_token").exchange().expectStatus().isUnauthorized();

        assertEquals(workspace.getActive(), workspaceRepository.findByIdOrThrow(workspace.getId()).getActive());
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();
    }
}
