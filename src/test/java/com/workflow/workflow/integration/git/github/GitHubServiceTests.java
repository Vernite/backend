package com.workflow.workflow.integration.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.Issue;
import com.workflow.workflow.integration.git.PullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubBranch;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubMergeInfo;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubUser;
import com.workflow.workflow.integration.git.github.data.InstallationToken;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.integration.git.github.entity.GitHubTaskKey;
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
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubServiceTests {
    public static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GitHubService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
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

        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = project.getStatuses().get(0);
        statuses[1] = project.getStatuses().get(2);
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        ReflectionTestUtils.setField(service, "client", WebClient.create("http://localhost:" + mockBackEnd.getPort()));
    }

    @AfterAll
    void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void createIssueTest() {
        installation.setSuspended(true);
        installation = installationRepository.save(installation);

        Task task = taskRepository.save(new Task(1, "name", "description", statuses[0], user, 0));

        Issue issue = service.createIssue(task).block();

        assertEquals(null, issue);

        Project newProject = projectRepository.save(new Project("NEW_NAME"));
        Status newStatus = statusRepository.save(new Status(67, "NEW_NAME", 1, false, true, 0, newProject));
        task = taskRepository.save(new Task(2, "name", "description", newStatus, user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 0));

        issue = service.createIssue(task).block();

        assertEquals(null, issue);

        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void patchIssueTest() throws JsonProcessingException {
        installation.setSuspended(true);
        installation = installationRepository.save(installation);

        Task task = taskRepository.save(new Task(3, "name", "description", statuses[0], user, 0));

        Issue issue = service.patchIssue(task).block();

        assertEquals(null, issue);

        Project newProject = projectRepository.save(new Project("NEW_NAME"));
        Status newStatus = statusRepository.save(new Status(123124, "NEW_NAME", 1, false, true, 0, newProject));
        Task task2 = taskRepository.save(new Task(4, "name", "description", newStatus, user, 0));
        gitHubTaskRepository.save(new GitHubTask(task2, integration, 1, (byte) 0));

        issue = service.patchIssue(task2).block();

        assertEquals(null, issue);

        installation.setSuspended(false);
        installation = installationRepository.save(installation);

        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 0));

        GitHubIssue gitIssue = new GitHubIssue(1, "url", "open", "name", "description");

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(gitIssue)).addHeader("Content-Type",
                "application/json"));

        Issue result = service.patchIssue(task).block();

        task.setAssignee(user);
        tokenCheck();
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(List.of())).addHeader("Content-Type",
                "application/json"));
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(gitIssue)).addHeader("Content-Type",
                "application/json"));

        result = service.patchIssue(task).block();

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(List.of(new GitHubUser(1, "username")))).addHeader("Content-Type",
                        "application/json"));
        mockBackEnd.enqueue(new MockResponse().setBody(MAPPER.writeValueAsString(gitIssue)).addHeader("Content-Type",
                "application/json"));

        result = service.patchIssue(task).block();

        assertEquals(gitIssue.getUrl(), result.getUrl());
        assertEquals(gitIssue.getState(), result.getState());
        assertEquals(gitIssue.getTitle(), result.getTitle());
        assertEquals(gitIssue.getBody(), result.getDescription());
    }

    @Test
    void getIssuesTest() {
        installation.setSuspended(true);
        installation = installationRepository.save(installation);

        List<Issue> issue = service.getIssues(project).collectList().block();

        assertEquals(0, issue.size());

        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void connectIssueTest() {
        Project newProject = projectRepository.save(new Project("NEW_NAME"));
        Status newStatus = statusRepository.save(new Status(213214124, "NEW_NAME", 1, false, true, 0, newProject));
        Task task = taskRepository.save(new Task(5, "name", "description", newStatus, user, 0));

        Issue issue = service.connectIssue(task, new Issue(1, "url", "title", "description", "github")).block();

        assertEquals(null, issue);

        task = taskRepository.save(new Task(6, "name", "description", statuses[0], user, 0));
        installation.setSuspended(true);
        installation = installationRepository.save(installation);
        issue = service.connectIssue(task, new Issue(1, "url", "title", "description", "github")).block();

        assertEquals(null, issue);
        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void deleteIssueTest() {
        Task task = taskRepository.save(new Task(7, "name", "description", statuses[0], user, 0));
        GitHubTask gitHubTask = new GitHubTask(task, integration, 1, (byte) 0);
        gitHubTask.setActive(new Date());
        gitHubTaskRepository.save(gitHubTask);
        service.deleteIssue(task);
        assertEquals(true, gitHubTaskRepository.findById(new GitHubTaskKey(task, integration)).isPresent());
    }

    @Test
    void getPullRequests() {
        installation.setSuspended(true);
        installation = installationRepository.save(installation);

        List<PullRequest> pullRequests = service.getPullRequests(project).collectList().block();

        assertEquals(0, pullRequests.size());

        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void connectPullRequest() {
        Project newProject = projectRepository.save(new Project("NEW_NAME"));
        Status newStatus = statusRepository.save(new Status(563434, "NEW_NAME", 1, false, true, 0, newProject));
        Task task = taskRepository.save(new Task(8, "name", "description", newStatus, user, 0));

        PullRequest pull = service
                .connectPullRequest(task, new PullRequest(1, "url", "title", "description", "github", "ref")).block();

        assertEquals(null, pull);

        task = taskRepository.save(new Task(9, "name", "description", statuses[0], user, 0));
        installation.setSuspended(true);
        installation = installationRepository.save(installation);
        pull = service.connectPullRequest(task, new PullRequest(1, "url", "title", "description", "github", "ref"))
                .block();

        assertEquals(null, pull);
        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void patchPullRequest() throws JsonProcessingException {
        Task task = taskRepository.save(new Task(10, "name", "description", statuses[0], user, 0));
        gitHubTaskRepository.save(new GitHubTask(task, integration, 1, (byte) 1));
        installation.setSuspended(true);
        installation = installationRepository.save(installation);

        Issue issue = service.patchPullRequest(task).block();

        assertEquals(null, issue);

        installation.setSuspended(false);
        installation = installationRepository.save(installation);

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(
                        new GitHubPullRequest(1, "url", "open", "name", "description", new GitHubBranch("ref"))))
                .addHeader("Content-Type",
                        "application/json"));

        issue = service.patchPullRequest(task).block();

        assertEquals(1, issue.getId());
        assertEquals("url", issue.getUrl());
        assertEquals("open", issue.getState());
        assertEquals("name", issue.getTitle());

        task.setAssignee(user);
        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(List.of())).addHeader("Content-Type", "application/json"));
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(
                        new GitHubPullRequest(1, "url", "open", "name", "description", new GitHubBranch("ref"))))
                .addHeader("Content-Type", "application/json"));
        issue = service.patchPullRequest(task).block();

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(List.of(new GitHubUser(1, "username"))))
                .addHeader("Content-Type", "application/json"));
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(
                        new GitHubPullRequest(1, "url", "open", "name", "description", new GitHubBranch("ref"))))
                .addHeader("Content-Type", "application/json"));
        issue = service.patchPullRequest(task).block();

        task.setStatus(statuses[1]);
        tokenCheck();
        mockBackEnd.enqueue(
                new MockResponse().setBody(MAPPER.writeValueAsString(new GitHubMergeInfo("1", false, "message")))
                        .addHeader("Content-Type",
                                "application/json"));
        issue = service.patchPullRequest(task).block();

        assertEquals(null, issue);

        assertEquals(true, gitHubTaskRepository.findById(new GitHubTaskKey(task, integration)).isPresent());
        assertEquals(true, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 2).isEmpty());

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(new GitHubMergeInfo("1", true, "message"))).addHeader("Content-Type",
                        "application/json"));
        issue = service.patchPullRequest(task).block();

        assertEquals(null, issue);

        assertEquals(true, gitHubTaskRepository.findById(new GitHubTaskKey(task, integration)).isPresent());
        assertEquals(true, gitHubTaskRepository.findByTaskAndActiveNullAndIsPullRequest(task, (byte) 2).isPresent());
    }
}
