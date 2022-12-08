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

package dev.vernite.vernite.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import java.util.List;

import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.meeting.Meeting;
import dev.vernite.vernite.meeting.MeetingRepository;
import dev.vernite.vernite.projectworkspace.ProjectMember;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceKey;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.release.Release;
import dev.vernite.vernite.release.ReleaseRepository;
import dev.vernite.vernite.sprint.Sprint;
import dev.vernite.vernite.sprint.SprintRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.task.time.TimeTrack;
import dev.vernite.vernite.task.time.TimeTrackRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
class ProjectControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private TimeTrackRepository timeTrackRepository;

    private User user;
    private UserSession session;
    private Workspace workspace;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_projects_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
    }

    @BeforeEach
    void reset() {
        projectRepository.deleteAll();
    }

    @Test
    void createSuccess() {
        CreateProject request = new CreateProject("POST", "", workspace.getId().getId());
        Project result = client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request).exchange().expectStatus().isOk().expectBody(Project.class).returnResult()
                .getResponseBody();

        assertNotNull(result);
        Project project = projectRepository.findByIdOrThrow(result.getId());

        assertEquals(result, project);
        assertEquals(0, project.getStatuses().size());
        assertEquals(1, project.getProjectWorkspaces().size());
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject()).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject("NAME", "", null)).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject(null, "", workspace.getId().getId())).exchange().expectStatus()
                .isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject("", "", null)).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject("a".repeat(51), "", null)).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject("a", "a".repeat(1001), null)).exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateProject("POST", "", 1000L)).exchange().expectStatus().isNotFound();
    }

    @Test
    void getSuccess() {
        Project project = projectRepository.save(new Project("GET"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBody(Project.class).isEqualTo(project);
    }

    @Test
    void getUnauthorized() {
        client.get().uri("/project/{id}", 1).exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("GET"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{id}", -1).cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("GET"));
        client.get().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject()).exchange().expectStatus().isOk().expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        project.setName("NEW PUT");
        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("NEW PUT", "", null)).exchange().expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        Workspace newWorkspace = workspaceRepository.save(new Workspace(2, "New Workspace", user));
        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject(null, "", 2L)).exchange().expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));
        assertEquals(1, workspaceRepository.findByIdOrThrow(newWorkspace.getId()).getProjectWorkspaces().size());
        assertEquals(0, workspaceRepository.findByIdOrThrow(workspace.getId()).getProjectWorkspaces().size());
    }

    @Test
    void updateBadRequest() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("  ", "", null)).exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("a".repeat(51), "", null)).exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        UpdateProject request = new UpdateProject("PUT", "", 1L);
        client.put().uri("/project/1").bodyValue(request).exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId()).contentType(MediaType.APPLICATION_JSON).bodyValue(request)
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        Project project = projectRepository.save(new Project("PUT"));

        client.put().uri("/project/1").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("PUT", "", 1L)).exchange().expectStatus().isNotFound();

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("PUT", "", 1L)).exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateProject("PUT", "", 2L)).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk();
        assertNotEquals(null, projectRepository.findById(project.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        client.delete().uri("/project/1").exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));

        client.delete().uri("/project/{id}", project.getId()).exchange().expectStatus().isUnauthorized();
        assertEquals(project.getActive(), projectRepository.findByIdOrThrow(project.getId()).getActive());
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/1").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("DELETE"));
        client.delete().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    @Deprecated
    void moveProjectWorkspaceNotFound() {
        client.put().uri("/project/{id}/workspace/1", 1024)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("PUT"));

        client.put().uri("/project/{id}/workspace/{wId}", project.getId(), 1024)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}/workspace/{wId}", project.getId(), 1024)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getProjectMembersSuccess() {
        Project project = projectRepository.save(new Project("PROJECT"));
        ProjectWorkspace ps = projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(ProjectMember.class).hasSize(1).contains(ps.getProjectMember());
    }

    @Test
    void getProjectMembersUnauthorized() {
        client.get().uri("/project/1/member").exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PROJECT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/member", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getProjectMembersNotFound() {
        client.get().uri("/project/1/member").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.get().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getProjectMemberSuccess() {
        Project project = projectRepository.save(new Project("PROJECT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        ProjectMember pm = client.get().uri("/project/{id}/member/{memberId}", project.getId(), user.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(ProjectMember.class).returnResult().getResponseBody();

        assertNotNull(pm);
        assertEquals(pm.user().getId(), user.getId());
        assertEquals(pm.user().getUsername(), user.getUsername());
    }

    @Test
    void getProjectMemberUnauthorized() {
        client.get().uri("/project/1/member/{memberId}", user.getId()).exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PROJECT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/member/{memberId}", project.getId(), user.getId()).exchange().expectStatus()
                .isUnauthorized();
    }

    @Test
    void getProjectMemberNotFound() {
        client.get().uri("/project/1/member/{memberId}", user.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.get().uri("/project/{id}/member/{memberId}", project.getId(), user.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        client.get().uri("/project/{id}/member/{memberId}", project.getId(), 0)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void addProjectMemberSuccess() {
        Project project = projectRepository.save(new Project("MEMBER"));

        User user2 = userRepository.findByUsername("member_add_test_name");
        if (user2 == null) {
            user2 = userRepository.save(new User("1", "2", "member_add_test_name", "member_add_test@Dname", "1"));
        }

        ProjectInvite invite = new ProjectInvite();
        invite.setEmails(List.of(user2.getEmail()));
        invite.setProjects(List.of(project.getId()));

        ProjectInvite result = client.post().uri("/project/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(invite).exchange().expectStatus()
                .isOk().expectBody(ProjectInvite.class).returnResult().getResponseBody();

        assertNotNull(result);
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());
        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, "inbox", user2), project)).isEmpty());

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, "inbox", user2), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());
        assertEquals("member_add_test_name", result.getEmails().get(0));
        assertEquals(project.getId(), result.getProjectList().get(0).getId());

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
        assertNotNull(result);

        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, "inbox", user2), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());

        invite.setEmails(null);

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());

        invite.setProjects(null);

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
        assertNotNull(result);
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());
    }

    @Test
    void addProjectMemberUnauthorized() {
        client.post().uri("/project/member").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteMemberSuccess() {
        Project project = projectRepository.save(new Project("MEMBER"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        User user2 = userRepository.findByUsername("member_add_test_name");
        if (user2 == null) {
            user2 = userRepository.save(new User("1", "2", "member_add_test_name", "member_add_test@Dname", "1"));
        }
        Workspace workspace2 = workspaceRepository.save(new Workspace(1, "test", user2));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace2, 2L));

        List<User> result = client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(List.of(user2.getId(), 666, 54, user.getId())).exchange().expectStatus().isOk()
                .expectBodyList(User.class).returnResult().getResponseBody();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(user2.getName(), result.get(0).getName());
        assertEquals(user2.getId(), result.get(0).getId());
        assertEquals(false,
                projectWorkspaceRepository.findById(new ProjectWorkspaceKey(workspace2, project)).isPresent());
        assertEquals(true,
                projectWorkspaceRepository.findById(new ProjectWorkspaceKey(workspace, project)).isPresent());
    }

    @Test
    void deleteMemberUnauthorized() {
        Project project = projectRepository.save(new Project("MEMBER"));

        client.put().uri("/project/{id}/member", project.getId()).bodyValue(List.of()).exchange().expectStatus()
                .isUnauthorized();
    }

    @Test
    void deleteProjectMemberForbidden() {
        Project project = projectRepository.save(new Project("MEMBER"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 2L));

        client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(List.of()).exchange().expectStatus()
                .isForbidden();
    }

    @Test
    void deleteProjectMemberNotFound() {
        client.put().uri("/project/666/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(List.of()).exchange().expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of()).exchange().expectStatus().isNotFound();
    }

    @Test
    void leaveProjectSuccess() {
        Project project = projectRepository.save(new Project("MEMBER"));
        ProjectWorkspace pw = projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertEquals(false, projectWorkspaceRepository.findById(pw.getId()).isPresent());
    }

    @Test
    void leaveProjectUnauthorized() {
        client.delete().uri("/project/1/member").exchange().expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.delete().uri("/project/{id}/member", project.getId()).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void leaveProjectNotFound() {
        client.delete().uri("/project/666/member").cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));

        client.delete().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getTimeTracksSuccess(@Autowired TaskRepository taskRepository) {
        Project project = projectRepository.save(new Project("MEMBER"));
        Task task = taskRepository.save(new Task(1, "n", "d", project.getStatuses().get(0), user, 1));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/track", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(TimeTrack.class).hasSize(0);

        timeTrackRepository.save(new TimeTrack(user, task));

        client.get().uri("/project/{id}/track", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(TimeTrack.class).hasSize(1);
    }

    @Test
    void getTimeTracksUnauthorized() {
        client.get().uri("/project/1/track").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getTimeTracksNotFound() {
        client.get().uri("/project/666/track").cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.get().uri("/project/{id}/track", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void getEventsSuccess(@Autowired TaskRepository taskRepository, @Autowired MeetingRepository meetingRepository,
            @Autowired SprintRepository sprintRepository, @Autowired ReleaseRepository releaseRepository) {
        Project project = projectRepository.save(new Project("MEMBER"));
        Task task = taskRepository.save(new Task(1, "n", "d", project.getStatuses().get(0), user, 1));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(0);

        task.setDeadline(new Date(2));
        taskRepository.save(task);

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(1);

        meetingRepository.save(new Meeting(project, "1", "1", new Date(3), new Date(1001)));

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(2);

        meetingRepository.save(new Meeting(project, "1", "1", new Date(2001), new Date(3032)));

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(2);

        task.setDeadline(new Date(3000));
        taskRepository.save(task);

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(1);

        sprintRepository.save(new Sprint(1, "n", new Date(1), new Date(1000), Sprint.Status.CREATED, "d", project));
        sprintRepository.save(new Sprint(2, "n", new Date(2000), new Date(3000), Sprint.Status.CREATED, "d", project));

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(2);

        Release release = new Release("Name", project);
        release.setDeadline(new Date(500));
        releaseRepository.save(release);

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(3);

        client.get().uri("/project/{id}/events?from=1&to=1000&type=0", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(1);

        task.setEstimatedDate(new Date(100));
        taskRepository.save(task);

        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(4);
    }

    @Test
    void getEventsUnauthorized() {
        client.get().uri("/project/1/events?from=1&to=1000").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getTEventsNotFound() {
        client.get().uri("/project/666/events?from=1&to=1000").cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.get().uri("/project/{id}/events?from=1&to=1000", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }
}
