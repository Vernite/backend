package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;

import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.project.ProjectRequest;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
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
@TestPropertySource({"classpath:application-test.properties", "classpath:application.properties"})
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
                .cookie("session", session.getSession())
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
        ProjectRequest request = new ProjectRequest(null, workspace.getId().getId());
        client.post().uri("/project")
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setName("0".repeat(51));
        client.post().uri("/project")
                .cookie("session", session.getSession())
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
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getProjectSuccess() {
        Project project = projectRepository.save(new Project("GET"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri("/project/" + project.getId())
                .cookie("session", session.getSession())
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

        client.get().uri("/project/" + project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getProjectNotFound() {
        client.get().uri("/project/1")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void putProjectSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        ProjectRequest request = new ProjectRequest(null, 1L);

        client.put().uri("/project/" + project.getId())
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Project.class)
                .isEqualTo(project);
        assertEquals(project, projectRepository.findByIdOrThrow(project.getId()));

        request.setName("NEW PUT");
        project.setName(request.getName());
        client.put().uri("/project/" + project.getId())
                .cookie("session", session.getSession())
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

        client.put().uri("/project/" + project.getId())
                .cookie("session", session.getSession())
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

        client.put().uri("/project/" + project.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void putProjectNotFound() {
        client.get().uri("/project/1")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteProjectSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/project/" + project.getId())
                .cookie("session", session.getSession())
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
        client.delete().uri("/project/" + project.getId())
                .exchange()
                .expectStatus().isUnauthorized();

        assertEquals(project.getActive(), projectRepository.findByIdOrThrow(project.getId()).getActive());
    }

    @Test
    void deleteProjectNotFound() {
        client.delete().uri("/project/1")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void moveProjectWorkspaceSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        Workspace workspace2 = workspaceRepository.save(new Workspace(2, user, "Project Tests 2"));

        client.put().uri(String.format("/project/%d/workspace/%d", project.getId(), workspace2.getId().getId()))
                .cookie("session", session.getSession())
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
        client.put().uri(String.format("/project/%d/workspace/%d", project.getId(), workspace2.getId().getId()))
                .exchange()
                .expectStatus().isUnauthorized();

        assertEquals(1, workspaceRepository.findByIdOrThrow(workspace.getId()).getProjectsWithPrivileges().size());
        assertEquals(0, workspaceRepository.findByIdOrThrow(workspace2.getId()).getProjectsWithPrivileges().size());
    }

    @Test
    void moveProjectWorkspaceNotFound() {
        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.put().uri(String.format("/project/%d/workspace/%d", project.getId(), 1024))
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getProjectMembersSuccess() {
        Project project = projectRepository.save(new Project("PUT"));
        ProjectWorkspace ps = projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri(String.format("/project/%d/member", project.getId()))
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(ProjectMember.class)
                .hasSize(1)
                .contains(ps.getProjectMember());
    }

    @Test
    void getProjectMembersUnauthorized() {
        client.get().uri(String.format("/project/1/member"))
                .exchange()
                .expectStatus().isUnauthorized();

        Project project = projectRepository.save(new Project("PUT"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.get().uri(String.format("/project/%d/member", project.getId()))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getProjectMembersNotFound() {
        client.get().uri(String.format("/project/1/member"))
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
