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

package dev.vernite.vernite.integration.git.github;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubBranch;
import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.data.GitHubMergeInfo;
import dev.vernite.vernite.integration.git.github.data.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubUser;
import dev.vernite.vernite.integration.git.github.data.InstallationToken;
import dev.vernite.vernite.integration.git.github.model.Authorization;
import dev.vernite.vernite.integration.git.github.model.AuthorizationRepository;
import dev.vernite.vernite.integration.git.github.model.Installation;
import dev.vernite.vernite.integration.git.github.model.InstallationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.integration.git.github.model.TaskIntegration;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationId;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.status.StatusRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubServiceTests {
    public static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private GitHubService2 service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private InstallationRepository installationRepository;
    @Autowired
    private ProjectIntegrationRepository integrationRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskIntegrationRepository issueRepository;
    @Autowired
    private AuthorizationRepository repo;

    private User user;
    private Project project;
    private Status[] statuses = new Status[2];
    private Installation installation;
    private ProjectIntegration integration;
    private Workspace workspace;

    void tokenCheck() throws JsonProcessingException {
        tokenCheck(installation);
    }

    void tokenCheck(Installation iGitHubInstallation) throws JsonProcessingException {
        iGitHubInstallation = installationRepository.findById(iGitHubInstallation.getId()).orElseThrow();
        if (iGitHubInstallation.getExpires().before(Date.from(Instant.now()))) {
            mockBackEnd.enqueue(new MockResponse()
                    .setBody(MAPPER.writeValueAsString(
                            new InstallationToken("token" + iGitHubInstallation.getId(),
                                    Instant.now().plus(30, ChronoUnit.MINUTES).toString())))
                    .addHeader("Content-Type", "application/json"));
        }
    }

    @BeforeAll
    public void init() throws IOException {
        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = project.getStatuses().get(0);
        statuses[1] = project.getStatuses().get(2);

        installation = new Installation();
        installation.setId(1);
        installation.setSuspended(false);
        installation.setExpires(new Date(1));
        installation.setTargetType("null");
        installation = installationRepository.save(installation);

        var auth = new Authorization();
        auth.setAccessToken("token");
        auth.setRefreshToken("refresh");
        auth.setExpires(Date.from(Instant.now().plusSeconds(100)));
        auth.setUser(user);
        auth.setAvatarUrl("avatar");
        auth.setLogin("login");
        auth.setRefreshTokenExpires(Date.from(Instant.now().plusSeconds(100)));
        auth.setScope("scope");
        auth.setTokenType("type");
        auth.setId(1);
        repo.save(auth);

        integration = integrationRepository.save(new ProjectIntegration("username/repo", project, installation));
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        mockBackEnd = new MockWebServer();
        r.add("github.api.url", () -> "http://localhost:" + mockBackEnd.getPort());
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
        Status newStatus = statusRepository.save(new Status("NEW_NAME", 1, 0, false, true, newProject));
        task = taskRepository.save(new Task(2, "name", "description", newStatus, user, 0));
        issueRepository
                .save(new TaskIntegration(task, integration, 1, TaskIntegration.Type.ISSUE));

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
        Status newStatus = statusRepository.save(new Status("NEW_NAME", 1, 0, false, true, newProject));
        Task task2 = taskRepository.save(new Task(4, "name", "description", newStatus, user, 0));
        issueRepository
                .save(new TaskIntegration(task2, integration, 1, TaskIntegration.Type.ISSUE));

        issue = service.patchIssue(task2).block();

        assertEquals(null, issue);

        installation.setSuspended(false);
        installation = installationRepository.save(installation);

        issueRepository
                .save(new TaskIntegration(task, integration, 1, TaskIntegration.Type.ISSUE));

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
        Status newStatus = statusRepository.save(new Status("NEW_NAME", 1, 0, false, true, newProject));
        Task task = taskRepository.save(new Task(5, "name", "description", newStatus, user, 0));

        Issue issue = service.connectIssue(task, 1).block();

        assertEquals(null, issue);

        task = taskRepository.save(new Task(6, "name", "description", statuses[0], user, 0));
        installation.setSuspended(true);
        installation = installationRepository.save(installation);
        issue = service.connectIssue(task, 1).block();

        assertEquals(null, issue);
        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void deleteIssueTest() {
        Task task = taskRepository.save(new Task(7, "name", "description", statuses[0], user, 0));
        var gitHubTask = new TaskIntegration(task, integration, 1, TaskIntegration.Type.ISSUE);
        issueRepository.save(gitHubTask);
        service.deleteIssue(task);
        assertEquals(false,
                issueRepository.findById(
                        new TaskIntegrationId(task.getId(), integration.getId(), TaskIntegration.Type.ISSUE.ordinal()))
                        .isPresent());
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
        Status newStatus = statusRepository.save(new Status("NEW_NAME", 1, 0, false, true, newProject));
        Task task = taskRepository.save(new Task(8, "name", "description", newStatus, user, 0));

        PullRequest pull = service
                .connectPullRequest(task, 1).block();

        assertEquals(null, pull);

        task = taskRepository.save(new Task(9, "name", "description", statuses[0], user, 0));
        installation.setSuspended(true);
        installation = installationRepository.save(installation);
        pull = service.connectPullRequest(task, 1)
                .block();

        assertEquals(null, pull);
        installation.setSuspended(false);
        installation = installationRepository.save(installation);
    }

    @Test
    void patchPullRequest() throws JsonProcessingException {
        Task task = taskRepository.save(new Task(10, "name", "description", statuses[0], user, 0));
        issueRepository.save(new TaskIntegration(task, integration, 1, TaskIntegration.Type.PULL_REQUEST));
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

        assertEquals(true, issueRepository.findById(
                new TaskIntegrationId(task.getId(), integration.getId(), TaskIntegration.Type.PULL_REQUEST.ordinal()))
                .isPresent());

        tokenCheck();
        mockBackEnd.enqueue(new MockResponse()
                .setBody(MAPPER.writeValueAsString(new GitHubMergeInfo("1", true, "message"))).addHeader("Content-Type",
                        "application/json"));
        issue = service.patchPullRequest(task).block();

        assertEquals(null, issue);

        assertEquals(true, issueRepository.findById(
                new TaskIntegrationId(task.getId(), integration.getId(), TaskIntegration.Type.PULL_REQUEST.ordinal()))
                .isPresent());
    }
}
