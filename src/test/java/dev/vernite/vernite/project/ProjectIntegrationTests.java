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

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vernite.vernite.integration.git.Issue;
import dev.vernite.vernite.integration.git.PullRequest;
import dev.vernite.vernite.integration.git.github.api.model.AppToken;
import dev.vernite.vernite.integration.git.github.data.GitHubBranch;
import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.data.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.model.Installation;
import dev.vernite.vernite.integration.git.github.model.InstallationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class ProjectIntegrationTests {
    public static MockWebServer mockBackEnd;
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private ProjectController controller;
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private InstallationRepository installationRepository;
    @Autowired
    private ProjectIntegrationRepository integrationRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;

    private User user;
    private UserSession session;
    private Project project;
    private Status[] statuses = new Status[2];
    private Installation installation;
    private Workspace workspace;

    void tokenCheck() throws JsonProcessingException {
        tokenCheck(installation);
    }

    void tokenCheck(Installation iGitHubInstallation) throws JsonProcessingException {
        iGitHubInstallation = installationRepository.findById(iGitHubInstallation.getId()).orElseThrow();
        if (iGitHubInstallation.getExpires().before(Date.from(Instant.now()))) {
            mockBackEnd.enqueue(new MockResponse()
                    .setBody(MAPPER.writeValueAsString(
                            new AppToken("token" + iGitHubInstallation.getId(),
                                    Instant.now().plus(30, ChronoUnit.MINUTES).toString())))
                    .addHeader("Content-Type", "application/json"));
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) throws IOException {
        mockBackEnd = new MockWebServer();
        r.add("github.api.url", () -> "http://localhost:" + mockBackEnd.getPort());
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

        integrationRepository.save(new ProjectIntegration("username/repo", project, installation));

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
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

    }

    @AfterAll
    public void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @Test
    void getIssuesSuccess() throws JsonProcessingException {
        Project newProject = projectRepository.save(new Project("NAME", ""));
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

        Project newProject = projectRepository.save(new Project("NAME", ""));

        client.get().uri("/project/{id}/integration/git/issue", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getPullRequestSuccess() throws JsonProcessingException {
        Project newProject = projectRepository.save(new Project("NAME", ""));
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

        Project newProject = projectRepository.save(new Project("NAME", ""));

        client.get().uri("/project/{id}/integration/git/pull", newProject.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
