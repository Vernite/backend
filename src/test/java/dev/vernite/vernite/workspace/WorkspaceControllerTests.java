/*
 * BSD 2-Clause License
 * 
 * Copyright (c) 2022, [Aleksandra Serba, Marcin Czerniak, Bartosz Wawrzyniak, Adrian Antkowiak]
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package dev.vernite.vernite.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;
import java.util.Optional;

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

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;

/**
 * TODO: refactor this class
 */
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
        List<Workspace> workspaces = List.of(new Workspace(1, "Test 1", user), new Workspace(2, "Test 3", user),
                new Workspace(3, "Test 2", user));
        workspaceRepository.saveAll(workspaces);
        // Test non empty return list
        List<Workspace> result = client.get().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Workspace.class).hasSize(3).returnResult()
                .getResponseBody();
        assertNotNull(result);
        workspaceEquals(workspaces.get(0), result.get(0));
        workspaceEquals(workspaces.get(1), result.get(2));
        workspaceEquals(workspaces.get(2), result.get(1));
        // Test soft delete
        workspaceRepository.delete(workspaces.get(0));
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
                .bodyValue(new CreateWorkspace("POST")).exchange().expectStatus().isOk().expectBody(Workspace.class)
                .returnResult().getResponseBody();
        assertNotNull(workspace);
        Optional<Workspace> optional = workspaceRepository.findById(new WorkspaceId(workspace.getId().getId(), user.getId()));
        assertEquals(true, optional.isPresent());
        Workspace result = optional.get();
        workspaceEquals(result, workspace);
    }

    @Test
    void createBadRequest() {
        // Test null name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateWorkspace(null)).exchange().expectStatus().isBadRequest();
        // Test empty name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateWorkspace("")).exchange().expectStatus().isBadRequest();
        // Test too long name
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateWorkspace("a".repeat(51))).exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/workspace").cookie(AuthController.COOKIE_NAME, "invalid_token").exchange().expectStatus()
                .isUnauthorized();
        client.post().uri("/workspace").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, "GET", user));

        Workspace result = client.get().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Workspace.class).returnResult().getResponseBody();

        workspaceEquals(workspace, result);
    }

    @Test
    void getUnauthorized() {
        client.get().uri("/workspace/1").exchange().expectStatus().isUnauthorized();

        long id = workspaceRepository.save(new Workspace(1, "GET", user)).getId().getId();
        client.get().uri("/workspace/{id}", id).exchange().expectStatus().isUnauthorized();
        client.get().uri("/workspace/{id}", id).cookie(AuthController.COOKIE_NAME, "invalid_token").exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Workspace workspace = new Workspace(1, "GET", user);
        workspaceRepository.delete(workspace);
        client.get().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, "PUT", user));

        client.put().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateWorkspace()).exchange()
                .expectStatus().isOk();

        Workspace result = client.put().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateWorkspace("NEW PUT"))
                .exchange().expectStatus().isOk().expectBody(Workspace.class).returnResult().getResponseBody();
        workspace.setName("NEW PUT");
        workspaceEquals(workspace, result);
        workspaceEquals(workspace, workspaceRepository.findByIdOrThrow(workspace.getId()));
    }

    @Test
    void updateBadRequest() {
        long id = workspaceRepository.save(new Workspace(1, "PUT", user)).getId().getId();

        client.put().uri("/workspace/{id}", id)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateWorkspace("")).exchange()
                .expectStatus().isBadRequest();

        client.put().uri("/workspace/{id}", id)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateWorkspace("a".repeat(51))).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        client.put().uri("/workspace/1").bodyValue(new UpdateWorkspace("NEW PUT")).exchange().expectStatus()
                .isUnauthorized();

        long id = workspaceRepository.save(new Workspace(1, "PUT", user)).getId().getId();
        client.put().uri("/workspace/{id}", id).bodyValue(new UpdateWorkspace("NEW PUT")).exchange().expectStatus()
                .isUnauthorized();
        client.put().uri("/workspace/{id}", id).cookie(AuthController.COOKIE_NAME, "invalid_token")
                .bodyValue(new UpdateWorkspace("NEW PUT")).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        client.put().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateWorkspace("NEW PUT")).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess(@Autowired ProjectRepository pRepo, @Autowired ProjectWorkspaceRepository pwRepo) {
        Workspace workspace = workspaceRepository.save(new Workspace(1, "DELETE", user));
        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotEquals(false, workspaceRepository.findById(workspace.getId()).isEmpty());

        Project project = new Project("DELETE");
        project.setActive(new Date());
        project = pRepo.save(project);
        Workspace workspace2 = workspaceRepository.save(new Workspace(1, "DELETE", user));
        pwRepo.save(new ProjectWorkspace(project, workspace2, 1L));

        client.delete().uri("/workspace/{id}", workspace2.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotEquals(false, workspaceRepository.findById(workspace2.getId()).isEmpty());
    }

    @Test
    void deleteConflict(@Autowired ProjectRepository pRepo, @Autowired ProjectWorkspaceRepository pwRepo) {
        Project project = pRepo.save(new Project("DELETE"));
        Workspace workspace = workspaceRepository.save(new Workspace(1, "DELETE", user));
        pwRepo.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().is4xxClientError();
    }

    @Test
    void deleteUnauthorized() {
        client.delete().uri("/workspace/1").exchange().expectStatus().isUnauthorized();

        Workspace workspace = workspaceRepository.save(new Workspace(1, "DELETE", user));
        client.delete().uri("/workspace/{id}", workspace.getId().getId()).exchange().expectStatus().isUnauthorized();
        client.delete().uri("/workspace/{id}", workspace.getId().getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_token").exchange().expectStatus().isUnauthorized();

        workspaceRepository.findByIdOrThrow(workspace.getId());
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/workspace/1").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();
    }
}
