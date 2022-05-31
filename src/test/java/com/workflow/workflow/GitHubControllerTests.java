package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import java.util.Date;

import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    private User user;
    private User otherUser;
    private UserSession session;
    private Project project;
    private Status[] statuses = new Status[2];
    private GitHubInstallation installation;
    private GitHubInstallation otherInstallation;
    private GitHubIntegration integration;
    private Workspace workspace;

    @BeforeAll
    public void init() {
        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1")));
        otherUser = userRepository.findById(2L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username2", "Email2@test.pl", "2")));
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = statusRepository.save(new Status("NAME", 1, false, true, 0, project));
        statuses[1] = statusRepository.save(new Status("NAME", 1, true, false, 1, project));
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        otherInstallation = installationRepository.save(new GitHubInstallation(2, otherUser, "username2"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_github_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_github_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @Test
    void getInstallationsSuccess() {
        client.get().uri("/user/integration/github")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GitHubInstallation.class)
                .hasSize(1);
    }

    @Test
    void getInstallationsUnauthorized() {
        client.get().uri("/user/integration/github")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getRepositoriesSuccess() {
        // TODO
    }

    @Test
    void getRepositoriesUnauthorized() {
        client.get().uri("/user/integration/github/repository")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newInstallationSuccess() {
        // TODO
    }

    @Test
    void newInstallationUnauthorized() {
        client.post().uri("/user/integration/github")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newInstallationConflict() {
        client.post().uri("/user/integration/github?installationId=" + installation.getInstallationId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void deleteInstallationSuccess() {
        client.delete().uri("/user/integration/github/" + installation.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();
        assertEquals(true, installationRepository.findById(installation.getId()).isPresent());
    }

    @Test
    void deleteInstallationUnauthorized() {
        client.delete().uri("/user/integration/github/" + installation.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteInstallationNotFound() {
        client.delete().uri("/user/integration/github/0")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.delete().uri("/user/integration/github/" + otherInstallation.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void newIntegrationSuccess() {
        // TODO
    }

    @Test
    void newIntegrationBadRequest() {
        client.post().uri("/project/{id}/integration/github", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue("test/test")
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void newIntegrationUnauthorized() {
        client.post().uri("/project/{id}/integration/github", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newIntegrationNotFound() {
        client.post().uri("/project/{id}/integration/github", 666L)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue("test/test")
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));

        client.post().uri("/project/{id}/integration/github", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue("test/test")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteIntegrationSuccess() {
        client.delete().uri("/project/{id}/integration/github", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();
        assertNotEquals(null, integrationRepository.findById(integration.getId()).get().getActive());
    }

    @Test
    void deleteIntegrationUnauthorized() {
        client.delete().uri("/project/{id}/integration/github", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteIntegrationNotFound() {
        client.delete().uri("/project/{id}/integration/github", 666L)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));

        client.delete().uri("/project/{id}/integration/github", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        projectWorkspaceRepository.save(new ProjectWorkspace(newProject, workspace, 1L));

        client.delete().uri("/project/{id}/integration/github", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
