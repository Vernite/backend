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

package dev.vernite.vernite.meeting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.Instant;
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
public class MeetingControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private MeetingRepository meetingRepository;
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
    private User user2;
    private User user3;
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
        session.setSession("session_token_meeting_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_meeting_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
        project = projectRepository.save(new Project("Meeting Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        user2 = userRepository.findByUsername("Username2Meetings");
        if (user2 == null) {
            user2 = userRepository.save(new User("Name2", "Surname2", "Username2Meetings", "Username2Meetings", "1"));
        }
        projectWorkspaceRepository.save(
                new ProjectWorkspace(project, workspaceRepository.save(new Workspace(1, "Project Tests", user2)), 1L));
        user3 = userRepository.findByUsername("Username3Meetings");
        if (user3 == null) {
            user3 = userRepository.save(new User("Name2", "Surname2", "Username3Meetings", "Username3Meetings", "1"));
        }
    }

    @BeforeEach
    void clean() {
        meetingRepository.deleteAll();
    }

    void meetingEquals(Meeting s1, Meeting s2) {
        assertEquals(s1.getName(), s2.getName());
        assertEquals(s1.getDescription(), s2.getDescription());
        assertEquals(s1.getStartDate(), s2.getStartDate());
        assertEquals(s1.getEndDate(), s2.getEndDate());
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Meeting.class).hasSize(0);
        // Test non-empty return list
        List<Meeting> meetings = List.of(
                new Meeting(project, "Name 1", "Description", new Date(), Date.from(Instant.now().plusMillis(1000))),
                new Meeting(project, "Name 2", "Description", new Date(), Date.from(Instant.now().plusMillis(1000))),
                new Meeting(project, "Name 3", "Description", new Date(), Date.from(Instant.now().plusMillis(1000))));
        meetingRepository.saveAll(meetings);
        List<Meeting> result = client.get().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Meeting.class).hasSize(3).returnResult().getResponseBody();
        assertNotNull(result);
        meetingEquals(result.get(0), meetings.get(0));
        meetingEquals(result.get(1), meetings.get(1));
        meetingEquals(result.get(2), meetings.get(2));
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/meeting", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{projectId}/meeting", -1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.get().uri("/project/{projectId}/meeting", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        Meeting meeting = client.post().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().plusMillis(22)),
                        List.of(user2.getId(), user3.getId())))
                .exchange()
                .expectStatus().isOk().expectBody(Meeting.class).returnResult().getResponseBody();
        assertNotNull(meeting);
        Meeting result = meetingRepository.findByIdOrThrow(meeting.getId());
        meetingEquals(meeting, result);
        assertEquals(1, result.getParticipants().size());
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(
                        new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().minusMillis(22)),
                                null))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest(null, "desc", null, Date.from(Instant.now().plusMillis(22)), new Date(),
                        null))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{projectId}/meeting", project.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .bodyValue(new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().plusMillis(22)),
                        null))
                .exchange().expectStatus().isUnauthorized();
        client.post().uri("/project/{projectId}/meeting", project.getId())
                .bodyValue(new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().plusMillis(22)),
                        null))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        client.post().uri("/project/{projectId}/meeting", -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().plusMillis(22)),
                        null))
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.post().uri("/project/{projectId}/meeting", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, new Date(), Date.from(Instant.now().plusMillis(22)),
                        null))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void getSuccess() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        Meeting result = client.get().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Meeting.class).returnResult().getResponseBody();
        assertNotNull(result);
        meetingEquals(meeting, result);
    }

    @Test
    void getUnauthorized() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.get().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.get().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId()).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.get().uri("/project/{projectId}/meeting/{meetingId}", -1, meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        client.get().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.get().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));

        client.get().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        meeting.setName("name");
        meeting.setDescription("desc");
        Meeting result = client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, List.of(user2.getId(), user3.getId())))
                .exchange()
                .expectStatus().isOk().expectBody(Meeting.class).returnResult().getResponseBody();
        assertNotNull(result);
        meetingEquals(meeting, result);
        assertEquals(1, result.getParticipants().size());
    }

    @Test
    void updateBadRequest() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("", "desc", null, null, null, null))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("a".repeat(51), null, null, null, null, null))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", null, null, new Date(),
                        Date.from(Instant.now().minusMillis(1000)), null))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token")
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isUnauthorized();
        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.put().uri("/project/{projectId}/meeting/{meetingId}", -1, meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isNotFound();
        client.put().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.put().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));

        client.put().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new MeetingRequest("name", "desc", null, null, null, null))
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotNull(meetingRepository.findById(meeting.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid_session_token").exchange().expectStatus().isUnauthorized();
        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), meeting.getId())
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteNotFound() {
        Meeting meeting = meetingRepository.save(new Meeting(project, "Name", "Description", new Date(),
                Date.from(Instant.now().plusMillis(1000))));
        client.delete().uri("/project/{projectId}/meeting/{meetingId}", -1, meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project.getId(), -1)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));

        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));

        client.delete().uri("/project/{projectId}/meeting/{meetingId}", project2.getId(), meeting.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }
}
