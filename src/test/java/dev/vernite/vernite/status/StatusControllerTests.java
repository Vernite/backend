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

package dev.vernite.vernite.status;

import static org.junit.jupiter.api.Assertions.assertNull;
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

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

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
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
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
        assertNotNull(result);
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
        assertNotNull(result);
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

    @Test
    void deleteBadRequest(@Autowired TaskRepository taskRepository) {
        Status status = statusRepository.save(new Status(1, "name", 1, false, true, 0, project));
        taskRepository.save(new Task(1, "null", "null", status, user, 0));
        client.delete().uri("/project/{projectId}/status/{statusId}", project.getId(), status.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isBadRequest();
        assertNull(statusRepository.findById(status.getId()).get().getActive());
    }
}
