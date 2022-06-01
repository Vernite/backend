package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.GitController;
import com.workflow.workflow.integration.git.GitTaskService;
import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.GitHubService;
import com.workflow.workflow.integration.git.github.data.GitHubBranch;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.data.InstallationToken;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.integration.git.github.entity.GitHubTaskRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
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
public class GitControllerTests {
    public static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GitController controller;
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
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private GitHubTaskRepository gitHubTaskRepository;

    private User user;
    private UserSession session;
    private Project project;
    private Status[] statuses = new Status[2];
    private GitHubInstallation installation;
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

        GitTaskService service = (GitTaskService) ReflectionTestUtils.getField(controller, "service");
        GitHubService gService = (GitHubService) ReflectionTestUtils.getField(service, "gitHubService");
        ReflectionTestUtils.setField(gService, "client",
                WebClient.create("http://localhost:" + mockBackEnd.getPort()));

        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1")));
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = statusRepository.save(new Status("NAME", 1, false, true, 0, project));
        statuses[1] = statusRepository.save(new Status("NAME", 1, true, false, 1, project));
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_git_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_git_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

    }

    @AfterAll
    public void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getIssuesSuccess() throws JsonProcessingException {
        Project newProject = projectRepository.save(new Project("NAME"));
        projectWorkspaceRepository.save(new ProjectWorkspace(newProject, workspace, 1L));

        List<Issue> issues = controller.getIssues(user, newProject.getId()).collectList().block();

        assertEquals(0, issues.size());

        List<GitHubIssue> gitHubIssues = List.of(
                new GitHubIssue(1, "url", "open", "title", "body"),
                new GitHubIssue(2, "url", "closed", "title", "body"),
                new GitHubIssue(3, "url", "open", "title", "body"),
                new GitHubIssue(4, "url", "closed", "title", "body"));

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(gitHubIssues))
                .addHeader("Content-Type", "application/json"));

        issues = controller.getIssues(user, project.getId()).collectList().block();

        assertEquals(4, issues.size());
        assertEquals(gitHubIssues.get(0).getNumber(), issues.get(0).getId());
        assertEquals(gitHubIssues.get(0).getUrl(), issues.get(0).getUrl());
        assertEquals(gitHubIssues.get(0).getState(), issues.get(0).getState());
        assertEquals(gitHubIssues.get(0).getTitle(), issues.get(0).getTitle());
        assertEquals(gitHubIssues.get(0).getBody(), issues.get(0).getDescription());
    }

    @Test
    void getIssuesUnauthorized() {
        client.get().uri("/project/{id}/integration/git/issue", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getIssuesNotFound() {
        client.get().uri("/project/{id}/integration/git/issue", 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));

        client.get().uri("/project/{id}/integration/git/issue", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void newIssueSuccess() throws JsonProcessingException {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        tokenCheck();

        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(
                new GitHubIssue(1, "url", "open", "name", "description")))
                .addHeader("Content-Type", "application/json"));

        Issue issue = controller.newIssue(user, project.getId(), task.getId(), null).block();

        assertEquals(1, issue.getId());
        assertEquals("url", issue.getUrl());
        assertEquals("open", issue.getState());
        assertEquals("name", issue.getTitle());
        assertEquals("description", issue.getDescription());
        assertEquals("github", issue.getService());

        assertEquals(true, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).isPresent());
        gitHubTaskRepository.delete(gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).get());

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(
                new GitHubIssue(1, "url", "open", "name", "description")))
                .addHeader("Content-Type", "application/json"));
        Issue issue2 = controller
                .newIssue(user, project.getId(), task.getId(), new Issue(1, "url", "title", "description", "github"))
                .block();

        assertEquals(1, issue2.getId());
        assertEquals("url", issue2.getUrl());
        assertEquals("open", issue2.getState());
        assertEquals("name", issue2.getTitle());
        assertEquals("description", issue2.getDescription());
        assertEquals("github", issue2.getService());

        assertEquals(true, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).isPresent());
        gitHubTaskRepository.delete(gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).get());
    }

    @Test
    void newIssueUnauthorized() {
        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), 1)
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newIssueNotFound() {
        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", 666, 1)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", 666, task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));
        Status newStatus = statusRepository.save(new Status("name", 0, false, true, 0, newProject));
        Task task2 = taskRepository.save(new Task("name", "description", newStatus, user, 0));

        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", newProject.getId(), task2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.post().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), task2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteIssueSuccess() {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();

        assertEquals(false, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).isPresent());
    }

    @Test
    void deleteIssueUnauthorized() {
        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), 21)
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteIssueNotFound() {
        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", 666, 21)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", 666, task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));
        Status newStatus = statusRepository.save(new Status("name", 0, false, true, 0, newProject));
        Task task2 = taskRepository.save(new Task("name", "description", newStatus, user, 0));
        gitHubTaskRepository.save(new GitHubTask(task2, integration, 1, (byte) 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", newProject.getId(), task2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/issue", project.getId(), task2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getPullRequestSuccess() throws JsonProcessingException {
        Project newProject = projectRepository.save(new Project("NAME"));
        projectWorkspaceRepository.save(new ProjectWorkspace(newProject, workspace, 1L));

        List<PullRequest> pulls = controller.getPullRequests(user, newProject.getId()).collectList().block();

        assertEquals(0, pulls.size());

        List<GitHubPullRequest> gitHubPulls = List.of(
                new GitHubPullRequest(1, "url", "open", "title", "body", new GitHubBranch("ref")),
                new GitHubPullRequest(2, "url", "closed", "title", "body", new GitHubBranch("ref")),
                new GitHubPullRequest(3, "url", "open", "title", "body", new GitHubBranch("ref")),
                new GitHubPullRequest(4, "url", "closed", "title", "body", new GitHubBranch("ref")));

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(gitHubPulls))
                .addHeader("Content-Type", "application/json"));

        pulls = controller.getPullRequests(user, project.getId()).collectList().block();

        assertEquals(4, pulls.size());
        assertEquals(gitHubPulls.get(0).getNumber(), pulls.get(0).getId());
        assertEquals(gitHubPulls.get(0).getUrl(), pulls.get(0).getUrl());
        assertEquals(gitHubPulls.get(0).getState(), pulls.get(0).getState());
        assertEquals(gitHubPulls.get(0).getTitle(), pulls.get(0).getTitle());
        assertEquals(gitHubPulls.get(0).getBody(), pulls.get(0).getDescription());
    }

    @Test
    void getPullRequestUnauthorized() {
        client.get().uri("/project/{id}/integration/git/pull", project.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getPullRequestNotFound() {
        client.get().uri("/project/{id}/integration/git/pull", 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));

        client.get().uri("/project/{id}/integration/git/pull", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void newPullRequestSuccess() throws JsonProcessingException {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(
                new GitHubPullRequest(1, "url", "open", "title", "body", new GitHubBranch("ref"))))
                .addHeader("Content-Type", "application/json"));

        PullRequest pull = controller.newPullRequest(user, project.getId(), task.getId(),
                new PullRequest(1, "url", "title", "body", "github", "ref")).block();

        assertEquals(1, pull.getId());
        assertEquals("url", pull.getUrl());
        assertEquals("open", pull.getState());
        assertEquals("title", pull.getTitle());
        assertEquals("body", pull.getDescription());
        assertEquals("github", pull.getService());
        assertEquals("ref", pull.getBranch());

        assertEquals(true, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 1).isPresent());
        gitHubTaskRepository.delete(gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 1).get());
    }

    @Test
    void newPullRequestUnauthorized() {
        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), 666)
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newPullRequestNotFound() {
        PullRequest pull = new PullRequest(1, "url", "title", "body", "github", "ref");

        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", 666, 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(pull)
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));
        Status newStatus = statusRepository.save(new Status("name", 0, false, true, 0, newProject));
        Task task = taskRepository.save(new Task("name", "description", newStatus, user, 0));

        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(pull)
                .exchange()
                .expectStatus().isNotFound();

        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", newProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(pull)
                .exchange()
                .expectStatus().isNotFound();

        task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        client.post().uri("/project/{id}/task/{taskId}/integration/git/pull", 666, task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(pull)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deletePullRequestSuccess() {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 1));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isOk();

        assertEquals(false, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 0).isPresent());
    }

    @Test
    void deletePullRequestUnauthorized() {
        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), 666)
                .exchange()
                .expectStatus().isUnauthorized();

        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 1));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), task.getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deletePullRequestNotFound() {
        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), 666)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Project newProject = projectRepository.save(new Project("NAME"));
        Status newStatus = statusRepository.save(new Status("name", 0, false, true, 0, newProject));
        Task task = taskRepository.save(new Task("name", "description", newStatus, user, 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", project.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", newProject.getId(), task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));

        client.delete().uri("/project/{id}/task/{taskId}/integration/git/pull", 666, task.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
