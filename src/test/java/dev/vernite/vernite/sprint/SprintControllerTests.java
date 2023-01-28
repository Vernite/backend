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

package dev.vernite.vernite.sprint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
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
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
        project = projectRepository.save(new Project("Sprint Tests", ""));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void clean() {
        sprintRepository.deleteAll();
    }

    void sprintEquals(Sprint s1, Sprint s2) {
        assertEquals(s1.getName(), s2.getName());
        assertEquals(s1.getDescription(), s2.getDescription());
        assertEquals(s1.getStartDate(), s2.getStartDate());
        assertEquals(s1.getFinishDate(), s2.getFinishDate());
        assertEquals(s1.getId(), s2.getId());
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Sprint.class).hasSize(0);
        // Test non-empty return list
        List<Sprint> sprints = List.of(
                new Sprint("Sprint 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project),
                new Sprint("Sprint 2", new Date(), new Date(), Sprint.Status.ACTIVE, "desc", project),
                new Sprint("Sprint 3", new Date(), new Date(), Sprint.Status.CLOSED, "desc", project));
        sprintRepository.saveAll(sprints);
        List<Sprint> result = client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Sprint.class).hasSize(3).returnResult().getResponseBody();
        assertNotNull(result);
        sprintEquals(result.get(0), sprints.get(0));
        sprintEquals(result.get(1), sprints.get(1));
        sprintEquals(result.get(2), sprints.get(2));

        result = client.get().uri("/project/{projectId}/sprint?status=0", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Sprint.class).hasSize(1).returnResult().getResponseBody();
        assertNotNull(result);
        sprintEquals(result.get(0), sprints.get(0));

        result = client.get().uri("/project/{projectId}/sprint?status=1", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Sprint.class).hasSize(1).returnResult().getResponseBody();
        assertNotNull(result);
        sprintEquals(result.get(0), sprints.get(1));

        result = client.get().uri("/project/{projectId}/sprint?status=2", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Sprint.class).hasSize(1).returnResult().getResponseBody();
        assertNotNull(result);
        sprintEquals(result.get(0), sprints.get(2));
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/sprint", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{projectId}/sprint", -1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));

        client.get().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        Sprint sprint = client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(
                        new CreateSprint("Sprint 1", "desc", new Date(), new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isOk().expectBody(Sprint.class).returnResult().getResponseBody();
        assertNotNull(sprint);
        Sprint result = sprintRepository.findByIdAndProjectOrThrow(sprint.getId(), project);
        sprintEquals(sprint, result);
    }

    @Test
    void createBadRequest() {
        // Test missing name
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint(null, "", new Date(), new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus()
                .isBadRequest();
        // Test empty name
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("  ", "", new Date(), new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus()
                .isBadRequest();
        // Test too long name
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(
                        new CreateSprint("a".repeat(51), "", new Date(), new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isBadRequest();
        // Test missing description
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("Sprint 1", null, new Date(), new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isBadRequest();
        // Test missing start date
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("Sprint 1", "", null, new Date(), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isBadRequest();
        // Test missing finish date
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("Sprint 1", "", new Date(), null, Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isBadRequest();
        // Test invalid dates
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("Sprint 1", "", new Date(),
                        Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), Sprint.Status.CREATED.ordinal()))
                .exchange()
                .expectStatus().isBadRequest();
        // Test missing status
        client.post().uri("/project/{projectId}/sprint", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateSprint("Sprint 1", "", new Date(), new Date(), null)).exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));
        var request = new CreateSprint("Sprint 1", "desc", new Date(), new Date(), Sprint.Status.CREATED.ordinal());
        client.post().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").bodyValue(request).exchange()
                .expectStatus().isUnauthorized();
        client.post().uri("/project/{projectId}/sprint", project.getId()).bodyValue(request).exchange().expectStatus()
                .isUnauthorized();
    }

    @Test
    void createNotFound() {
        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));
        var request = new CreateSprint("Sprint 1", "desc", new Date(), new Date(), Sprint.Status.CREATED.ordinal());
        client.post().uri("/project/{projectId}/sprint", -1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request).exchange().expectStatus().isNotFound();
        client.post().uri("/project/{projectId}/sprint", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
    }

    @Test
    void getSuccess() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        Sprint result = client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Sprint.class).returnResult().getResponseBody();
        sprintEquals(sprint, result);
    }

    @Test
    void getUnauthorized() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("S 2", new Date(), new Date(), Sprint.Status.CREATED, "desc", project2));
        client.get().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        var request = new UpdateSprint("S2", "desc2", new Date(), new Date(), Sprint.Status.CREATED.ordinal());
        sprint.update(request);
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isOk();
        Sprint updatedSprint = sprintRepository.findByIdAndProjectOrThrow(sprint.getId(), project);
        sprintEquals(sprint, updatedSprint);
    }

    @Test
    void updateBadRequest() {
        var sprint = sprintRepository
                .save(new Sprint("S1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateSprint("  ", null, null, null, null))
                .exchange().expectStatus().isBadRequest();
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateSprint("a".repeat(51), null, null, null, null))
                .exchange().expectStatus().isBadRequest();
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(
                        new UpdateSprint(null, null, null, Date.from(Instant.now().minus(1, ChronoUnit.DAYS)), null))
                .exchange().expectStatus().isBadRequest();
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateSprint(null, null, Date.from(Instant.now().plus(1, ChronoUnit.DAYS)), null, null))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        var request = new UpdateSprint("Sprint 2", "desc", new Date(), new Date(), Sprint.Status.CREATED.ordinal());
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").bodyValue(request).exchange()
                .expectStatus().isUnauthorized();
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId()).bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        var request = new UpdateSprint("Sprint 1", "desc", new Date(), new Date(), Sprint.Status.CREATED.ordinal());
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("S 2", new Date(), new Date(), Sprint.Status.CREATED, "desc", project2));
        client.put().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();

        client.put().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(request).exchange().expectStatus()
                .isNotFound();
    }

    @Test
    void deleteSuccess() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk();
        assertFalse(sprintRepository.findById(sprint.getId()).isPresent());
    }

    @Test
    void deleteUnauthorized() {
        Sprint sprint = sprintRepository
                .save(new Sprint("S 1", new Date(), new Date(), Sprint.Status.CREATED, "desc", project));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        Project project2 = projectRepository.save(new Project("Sprint Tests 2", ""));
        Sprint sprint2 = sprintRepository
                .save(new Sprint("S 2", new Date(), new Date(), Sprint.Status.CREATED, "desc", project2));
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project2.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        client.delete().uri("/project/{projectId}/sprint/{sprintId}", project.getId(), sprint2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }
}
