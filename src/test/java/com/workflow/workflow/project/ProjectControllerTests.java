package com.workflow.workflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;

import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceKey;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
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
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
    }

    @BeforeEach
    void reset() {
        projectRepository.deleteAll();
    }

    @Test
    void createSuccess() {
        ProjectRequest request = new ProjectRequest("POST", workspace.getId().getId());
        Project result = client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(request).exchange().expectStatus().isOk().expectBody(Project.class).returnResult()
                .getResponseBody();

        Project project = projectRepository.findByIdOrThrow(result.getId());

        assertEquals(result, project);
        assertEquals(3, project.getStatuses().size());
        assertEquals(1, project.getProjectWorkspaces().size());
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest()).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("NAME", null)).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest(null, workspace.getId().getId())).exchange().expectStatus()
                .isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("", null)).exchange().expectStatus().isBadRequest();

        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("a".repeat(51), null)).exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        client.post().uri("/project").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("POST", -1L)).exchange().expectStatus().isNotFound();
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
                .bodyValue(new ProjectRequest()).exchange().expectStatus().isOk().expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        project.setName("NEW PUT");
        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("NEW PUT", null)).exchange().expectStatus().isOk()
                .expectBody(Project.class).isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        Workspace newWorkspace = workspaceRepository.save(new Workspace(2, user, "New Workspace"));
        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest(null, 2L)).exchange().expectStatus().isOk()
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
                .bodyValue(new ProjectRequest("  ", null)).exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("a".repeat(51), null)).exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        ProjectRequest request = new ProjectRequest("PUT", 1L);
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
                .bodyValue(new ProjectRequest("PUT", 1L)).exchange().expectStatus().isNotFound();

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("PUT", 1L)).exchange().expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ProjectRequest("PUT", -1L)).exchange().expectStatus().isNotFound();
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
    void moveProjectWorkspaceSuccess() {
        Project project = projectRepository.save(new Project("DELETE"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        Workspace workspace2 = workspaceRepository.save(new Workspace(2, user, "Project Tests 2"));

        client.put().uri("/project/{id}/workspace/{wId}", project.getId(), workspace2.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, workspaceRepository.findByIdOrThrow(workspace2.getId()).getProjectsWithPrivileges().size());
        assertEquals(0, workspaceRepository.findByIdOrThrow(workspace.getId()).getProjectsWithPrivileges().size());

        Workspace inbox = workspaceRepository.save(new Workspace(0, user, "inbox"));

        Project project2 = projectRepository.save(new Project("DELETE"));
        Project project3 = projectRepository.save(new Project("DELETE"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project2, inbox, 1L));
        projectWorkspaceRepository.save(new ProjectWorkspace(project3, inbox, 1L));

        client.put().uri("/project/{id}/workspace/{wId}", project2.getId(), workspace2.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();

        assertEquals(1, workspaceRepository.findByIdOrThrow(inbox.getId()).getProjectsWithPrivileges().size());

        client.put().uri("/project/{id}/workspace/{wId}", project3.getId(), workspace2.getId().getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();

        assertEquals(false, workspaceRepository.findById(inbox.getId()).isPresent());

        assertEquals(3, workspaceRepository.findByIdOrThrow(workspace2.getId()).getProjectsWithPrivileges().size());
    }

    @Test
    @Deprecated
    void moveProjectWorkspaceUnauthorized() {
        client.put().uri("/project/1/workspace/1")
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        Workspace workspace2 = workspaceRepository.save(new Workspace(2, user, "Project Tests 2"));
        client.put().uri("/project/{id}/workspace/{wId}", project.getId(), workspace2.getId().getId())
                .exchange()
                .expectStatus().isUnauthorized();

        assertEquals(1, workspaceRepository.findByIdOrThrow(workspace.getId()).getProjectsWithPrivileges().size());
        assertEquals(0, workspaceRepository.findByIdOrThrow(workspace2.getId()).getProjectsWithPrivileges().size());
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

        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());
        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isEmpty());

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();

        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());
        assertEquals("member_add_test_name", result.getEmails().get(0));
        assertEquals(project.getId(), result.getProjectList().get(0).getId());

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();

        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());

        invite.setEmails(null);

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());

        invite.setProjects(null);

        result = client.post().uri("/project/member").cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite).exchange().expectStatus().isOk().expectBody(ProjectInvite.class).returnResult()
                .getResponseBody();
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
        Workspace workspace2 = workspaceRepository.save(new Workspace(1, user2, "test"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace2, 2L));

        List<User> result = client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(List.of(user2.getId(), 666, 54, user.getId())).exchange().expectStatus().isOk()
                .expectBodyList(User.class).returnResult().getResponseBody();
        assertEquals(1, result.size());
        assertEquals(user2.getName(), result.get(0).getName());
        assertEquals(user2.getId(), result.get(0).getId());
        assertEquals(false,
                projectWorkspaceRepository.findById(new ProjectWorkspaceKey(workspace2, project)).isPresent());
        assertEquals(true,
                projectWorkspaceRepository.findById(new ProjectWorkspaceKey(workspace, project)).isPresent());
    }

    @Test
    void deleteMemberUnautorized() {
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
}