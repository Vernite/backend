package com.workflow.workflow.integration.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.data.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationRepositories;
import com.workflow.workflow.integration.git.github.data.GitHubUser;
import com.workflow.workflow.integration.git.github.data.InstallationToken;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.AfterAll;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubControllerTests {
    public static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();
    
    @Autowired
    private WebTestClient client;
    @Autowired
    private GitHubController controller;
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
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    private User user;
    private User otherUser;
    private User noUser;
    private UserSession session;
    private Project project;
    private Status[] statuses = new Status[2];
    private GitHubInstallation installation;
    private GitHubInstallation otherInstallation;
    private GitHubIntegration integration;
    private Workspace workspace;

    void tokenCheck() throws JsonProcessingException {
        tokenCheck(installation);
    }

    void tokenCheck(GitHubInstallation iGitHubInstallation) throws JsonProcessingException {
        iGitHubInstallation = installationRepository.findByIdOrThrow(iGitHubInstallation.getId());
        if (iGitHubInstallation.getExpiresAt().before(Date.from(Instant.now()))) {
            mockBackEnd.enqueue(new MockResponse()
                    .setBody(MAPPER.writeValueAsString(
                            new InstallationToken("token" + iGitHubInstallation.getId(),
                                    Instant.now().plus(30, ChronoUnit.MINUTES).toString())))
                    .addHeader("Content-Type", "application/json"));
        }
    }

    @BeforeAll
    public void init() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();

        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        this.otherUser = userRepository.findByUsername("Username2");
        if (this.otherUser == null) {
            this.otherUser = userRepository.save(new User("Name", "Surname", "Username2", "Email2@test.pl", "1"));
        }
        this.noUser = userRepository.findByUsername("Username3");
        if (this.noUser == null) {
            this.noUser = userRepository.save(new User("Name", "Surname", "Username3", "Email3@test.pl", "1"));
        }
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = project.getStatuses().get(0);
        statuses[1] = project.getStatuses().get(2);
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        otherInstallation = installationRepository.save(new GitHubInstallation(2, otherUser, "username2"));
        GitHubInstallation sus = new GitHubInstallation(4523, user, "username4523");
        sus.setSuspended(true);
        installationRepository.save(sus);
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

        GitHubService service = (GitHubService) ReflectionTestUtils.getField(controller, "service");
        ReflectionTestUtils.setField(service, "client", WebClient.create("http://localhost:" + mockBackEnd.getPort()));
    }

    @AfterAll
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getInstallationsSuccess() {
        client.get().uri("/user/integration/github")
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(GitHubInstallation.class)
                .hasSize(2);
    }

    @Test
    void getInstallationsUnauthorized() {
        client.get().uri("/user/integration/github")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getRepositoriesSuccess() throws JsonProcessingException {
        GitHubInstallationRepositories list = new GitHubInstallationRepositories();
        list.setRepositories(List.of(
                new GitHubRepository(1, "username/test", true),
                new GitHubRepository(2, "username/repo2", false),
                new GitHubRepository(3, "username/test3", true)));
        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(list)).addHeader("Content-Type",
                "application/json"));

        GitHubIntegrationInfo info = controller.getRepositories(user).block();
        assertEquals(true, info.getGitRepositories().containsAll(list.getRepositories()));
    }

    @Test
    void getRepositoriesUnauthorized() {
        client.get().uri("/user/integration/github/repository")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newInstallationSuccess() throws JsonProcessingException {
        GitHubInstallationApi api = new GitHubInstallationApi(54, new GitHubUser(24, "username3"));
        GitHubInstallationRepositories list = new GitHubInstallationRepositories();
        list.setRepositories(List.of(
                new GitHubRepository(1, "username3/test", true),
                new GitHubRepository(2, "username3/repo2", false),
                new GitHubRepository(3, "username3/test3", true)));
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(api)).addHeader("Content-Type",
                "application/json"));
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(
                        new InstallationToken("token54", Instant.now().plus(30, ChronoUnit.MINUTES).toString())))
                .addHeader("Content-Type", "application/json"));
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(list)).addHeader("Content-Type",
                "application/json"));

        GitHubIntegrationInfo info = controller.newInstallation(noUser, 54).block();
        assertEquals(true, info.getGitRepositories().containsAll(list.getRepositories()));
        assertEquals(true, installationRepository.findByInstallationId(54).isPresent());
        assertEquals(api.getAccount().getLogin(),
                installationRepository.findByInstallationId(54).get().getGitHubUsername());
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
    void newIntegrationSuccess() throws JsonProcessingException {
        GitHubInstallation testInst = installationRepository.save(new GitHubInstallation(4532, user, "username4532"));
        GitHubInstallationRepositories fakeList = new GitHubInstallationRepositories();
        fakeList.setRepositories(List.of(
                new GitHubRepository(1, "Test/test", true),
                new GitHubRepository(2, "Test/test2", false),
                new GitHubRepository(3, "Test/test3", true)));
        GitHubInstallationRepositories list = new GitHubInstallationRepositories();
        list.setRepositories(List.of(
                new GitHubRepository(1, "username/test", true),
                new GitHubRepository(2, "username/repo2", false),
                new GitHubRepository(3, "username/test3", true)));
        for (GitHubInstallation installation2 : installationRepository.findByUser(user)) {
            if (!installation2.getSuspended()) {
                tokenCheck(installation2);
                if (installation2.getId() == installation.getId()) {
                    mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(list))
                            .addHeader("Content-Type", "application/json"));
                } else {
                    mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(fakeList))
                            .addHeader("Content-Type", "application/json"));
                }
            }
        }

        Project newProject = projectRepository.save(new Project("NAMEMEM"));
        projectWorkspaceRepository.save(new ProjectWorkspace(newProject, workspace, 1L));

        Project result = controller.newIntegration(user, newProject.getId(), "username/repo2").block();
        assertEquals(newProject.getId(), result.getId());
        assertEquals("username/repo2", projectRepository.findById(newProject.getId()).get().getGitHubIntegration());
        assertEquals(true, integrationRepository.findByProjectAndActiveNull(result).isPresent());
        installationRepository.delete(testInst);
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
