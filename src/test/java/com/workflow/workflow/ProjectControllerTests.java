package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;

import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectInvite;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.project.ProjectRequest;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWithPrivileges;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceKey;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
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
public class ProjectControllerTests {
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

    private User user;
    private UserSession session;
    private Workspace workspace;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1")));
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_projects_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
    }

    @BeforeEach
    void reset() {
        projectRepository.deleteAll();
    }

    @Test
    void newProjectSuccess() {
        ProjectRequest request = new ProjectRequest("POST", workspace.getId().getId());
        Project project = client.post().uri("/project")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .returnResult()
                .getResponseBody();
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));
    }

    @Test
    void newProjectBadRequest() {
        ProjectRequest request = new ProjectRequest();
        client.post().uri("/project")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setWorkspaceId(workspace.getId().getId());
        client.post().uri("/project")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setName("0".repeat(51));
        client.post().uri("/project")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void newProjectUnauthorized() {
        client.post().uri("/project")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newProjectNotFound() {
        ProjectRequest request = new ProjectRequest("POST", 1024L);
        client.post().uri("/project")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getProjectSuccess() {
        Project project = projectRepository.save(new Project("GET"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));
    }

    @Test
    void getProjectUnauthorized() {
        client.get().uri("/project/1")
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("GET"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getProjectNotFound() {
        client.get().uri("/project/1")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("GET"));
        client.get().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void putProjectSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        ProjectRequest request = new ProjectRequest(null, 1L);

        client.put().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        request.setName("NEW PUT");
        project.setName(request.getName());
        client.put().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));
    }

    @Test
    void putProjectBadRequest() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        ProjectRequest request = new ProjectRequest("0".repeat(51), 1L);

        client.put().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void putProjectUnauthorized() {
        ProjectRequest request = new ProjectRequest("PUT", 1L);
        client.put().uri("/project/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri("/project/{id}", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void putProjectNotFound() {
        ProjectRequest request = new ProjectRequest("PUT", 1L);
        Project project = projectRepository.save(new Project("PUT"));

        client.put().uri("/project/1")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();

        client.put().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteProjectSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();
        assertNotEquals(null, projectRepository.findById(project.getId()).get().getActive());
    }

    @Test
    void deleteProjectUnauthorized() {
        client.delete().uri("/project/1")
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        client.delete().uri("/project/{id}", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();

        assertEquals(project.getActive(), projectRepository.findByIdOrThrow(project.getId()).getActive());
    }

    @Test
    void deleteProjectNotFound() {
        client.delete().uri("/project/1")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("DELETE"));
        client.delete().uri("/project/{id}", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
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
    }

    @Test
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
        Project project = projectRepository.save(new Project("PUT"));
        ProjectWorkspace ps = projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProjectMember.class)
                .hasSize(1)
                .contains(ps.getProjectMember());
    }

    @Test
    void getProjectMembersUnauthorized() {
        client.get().uri("/project/1/member")
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/{id}/member", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getProjectMembersNotFound() {
        client.get().uri("/project/1/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.get().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
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
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectInvite.class)
                .returnResult()
                .getResponseBody();

        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());
        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isEmpty());

        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        result = client.post().uri("/project/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectInvite.class)
                .returnResult()
                .getResponseBody();

        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());
        assertEquals("member_add_test_name", result.getEmails().get(0));
        assertEquals(project.getId(), result.getProjectList().get(0).getId());

        result = client.post().uri("/project/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectInvite.class)
                .returnResult()
                .getResponseBody();

        assertEquals(true, projectWorkspaceRepository
                .findById(new ProjectWorkspaceKey(new Workspace(0, user2, "inbox"), project)).isPresent());
        assertEquals(1, result.getEmails().size());
        assertEquals(1, result.getProjectList().size());

        invite.setEmails(null);

        result = client.post().uri("/project/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectInvite.class)
                .returnResult()
                .getResponseBody();
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());

        invite.setProjects(null);

        result = client.post().uri("/project/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(invite)
                .exchange()
                .expectStatus().isOk()
                .expectBody(ProjectInvite.class)
                .returnResult()
                .getResponseBody();
        assertEquals(0, result.getEmails().size());
        assertEquals(0, result.getProjectList().size());
    }

    @Test
    void addProjectMemberUnauthorized() {
        client.post().uri("/project/member")
                .exchange()
                .expectStatus().isUnauthorized();
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
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of(user2.getId(), 666, 54))
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class)
                .returnResult()
                .getResponseBody();
        assertEquals(1, result.size());
        assertEquals(user2.getName(), result.get(0).getName());
        assertEquals(user2.getId(), result.get(0).getId());
        assertEquals(false, projectWorkspaceRepository.findById(new ProjectWorkspaceKey(workspace2, project)).isPresent());
    }

    @Test
    void deleteMemberUnautorized() {
        Project project = projectRepository.save(new Project("MEMBER"));

        client.put().uri("/project/{id}/member", project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteProjectMemberForbidden() {
        Project project = projectRepository.save(new Project("MEMBER"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 2L));

        client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of())
                .exchange()
                .expectStatus().isForbidden();
    }

    @Test
    void deleteProjectMemberNotFound() {
        client.put().uri("/project/666/member")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of())
                .exchange()
                .expectStatus().isNotFound();

        Project project = projectRepository.save(new Project("MEMBER"));
        client.put().uri("/project/{id}/member", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(List.of())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void projectTests() {
        Project project = new Project("name");
        Project other = new Project("other");

        assertEquals(false, project.equals(null));
        assertEquals(false, project.equals("name"));
        assertEquals(false, project.equals(other));
        assertEquals(true, project.compareTo(other) < 0);

        other.setName("name");
        other.setId(1);

        assertEquals(true, project.compareTo(other) < 0);
        assertEquals(false, project.equals(other));

        assertNotEquals(other.hashCode(), project.hashCode());

        other.setId(project.getId());
        assertEquals(true, project.equals(other));
        project.setName(null);
        assertEquals(false, project.equals(other));
        other.setName(null);
        assertEquals(true, project.equals(other));

        assertEquals(other.hashCode(), project.hashCode());

        project.setGitHubIntegration(new GitHubIntegration(project, null, "repository/name"));
        assertEquals("repository/name", project.getGitHubIntegration());
        project.setSprintCounter(new CounterSequence());
        assertEquals(0, project.getSprintCounter().getCounterValue());
        project.setStatusCounter(new CounterSequence());
        assertEquals(0, project.getStatusCounter().getCounterValue());
        project.setTaskCounter(new CounterSequence());
        assertEquals(0, project.getTaskCounter().getCounterValue());
        assertEquals(0, project.getStatuses().size());
        project.setStatuses(List.of(new Status()));
        assertEquals(1, project.getStatuses().size());
        project.setProjectWorkspaces(List.of(new ProjectWorkspace()));
        assertEquals(1, project.getProjectWorkspaces().size());
    }

    @Test
    void projectMemberTests() {
        ProjectMember member = new ProjectMember(user, 1L);
        ProjectMember other = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(null));
        assertEquals(false, member.equals("name"));
        assertEquals(true, member.equals(other));

        other = new ProjectMember(user, 2L);

        assertEquals(false, member.equals(other));

        other = new ProjectMember(null, 1L);

        assertEquals(false, member.equals(other));

        member = new ProjectMember(null, 1L);

        assertEquals(true, member.equals(other));

        other = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(other));

        User otherUser = new User();
        otherUser.setId(666L);
        other = new ProjectMember(otherUser, 1L);

        member = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(other));
    }

    @Test
    void projectWithPrivillagesTests() {
        Project project = new Project("name");
        Project other = new Project("other");

        ProjectWithPrivileges pwp = new ProjectWithPrivileges(project, 1L);
        ProjectWithPrivileges opwp = new ProjectWithPrivileges(other, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals("name"));
        assertEquals(false, pwp.equals(opwp));
        assertEquals(true, pwp.compareTo(opwp) < 0);
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        pwp = new ProjectWithPrivileges(null, 1L);

        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        opwp = new ProjectWithPrivileges(null, 1L);

        assertEquals(true, pwp.equals(opwp));

        opwp = new ProjectWithPrivileges(project, 2L);

        assertEquals(false, pwp.equals(opwp));
    }

    @Test
    void projectWorkspaceTests() {
        Project project = new Project("name");
        Project other = new Project("other");
        other.setId(333L);

        ProjectWorkspace pwp = new ProjectWorkspace(project, workspace, 1L);
        ProjectWorkspace opwp = new ProjectWorkspace(other, workspace, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals("name"));
        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        pwp.setId(null);

        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        opwp.setId(null);

        assertEquals(true, pwp.equals(opwp));

        opwp.setPrivileges(2L);

        assertEquals(false, pwp.equals(opwp));

        opwp.setProject(null);
        opwp.setWorkspace(null);

        assertEquals(false, pwp.equals(opwp));

        ProjectWorkspaceKey key = new ProjectWorkspaceKey(workspace, project);

        assertEquals(false, key.equals(null));
        assertEquals(false, key.equals("name"));
        assertEquals(0, key.compareTo(key));
        assertEquals(true, key.compareTo(new ProjectWorkspaceKey(workspace, other)) < 0);

        assertEquals(project.getId(), key.getProjectId());
        assertEquals(workspace.getId(), key.getWorkspaceId());

        key.setWorkspaceId(null);
        key.setProjectId(0);

        assertEquals(null, key.getWorkspaceId());
        assertEquals(0, key.getProjectId());

        assertNotEquals(new ProjectWorkspaceKey(workspace, project).hashCode(), key.hashCode());
        assertNotEquals(new ProjectWorkspaceKey(workspace, project), key);
        assertNotEquals(key, new ProjectWorkspaceKey(workspace, project));
        assertEquals(true, key.equals(key));
    }
}
