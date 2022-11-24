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

import java.util.List;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.vernite.vernite.integration.git.github.data.GitHubBranch;
import dev.vernite.vernite.integration.git.github.data.GitHubCommit;
import dev.vernite.vernite.integration.git.github.data.GitHubInstallationApi;
import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.data.GitHubPullRequest;
import dev.vernite.vernite.integration.git.github.data.GitHubRepository;
import dev.vernite.vernite.integration.git.github.data.GitHubUser;
import dev.vernite.vernite.integration.git.github.data.GitHubWebhookData;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallation;
import dev.vernite.vernite.integration.git.github.entity.GitHubInstallationRepository;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegrationRepository;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskIssueRepository;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskPull;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskPullRepository;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitHubWebhookServiceTests {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private GitHubTaskIssueRepository issueRepository;
    @Autowired
    private GitHubTaskPullRepository pullRepository;

    private HmacUtils utils;

    private User user;
    private Project project;
    private GitHubInstallation installation;
    private GitHubIntegration integration;

    @Value("${githubKey}")
    private void loadHmacUtils(String githubKey) {
        utils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, githubKey);
    }

    @BeforeAll
    void init() {
        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "contact@vernite.dev", "1"));
        }
        project = projectRepository.save(new Project("NAME"));
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
    }

    @BeforeEach
    void ensureUser() {
        User systemUser = userRepository.findByUsername("Username"); // TODO change system user
        if (systemUser == null) {
            systemUser = userRepository.save(new User("Name", "Surname", "Username", "contact@vernite.dev", "1"));
        }

    }

    @Test
    void githubUnauthorized() {
        client.post().uri("/webhook/github").header("X-Hub-Signature-256", "sha256=12345")
                .header("X-GitHub-Event", "push").bodyValue("{}").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void githubBadRequest() {
        client.post().uri("/webhook/github").header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("[]"))
                .header("X-GitHub-Event", "push").bodyValue("[]").exchange().expectStatus().isBadRequest();
    }

    @Test
    void githubSuccessRepositories() throws JsonProcessingException {
        // Test empty repositories remove
        client.post().uri("/webhook/github").header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("{}"))
                .header("X-GitHub-Event", "installation_repositories").bodyValue("{}").exchange().expectStatus().isOk();
        // Test not empty repositories remove
        GitHubWebhookData data = new GitHubWebhookData();
        GitHubIntegration mockIntegration = new GitHubIntegration(project, installation, "username/test");
        data.setRepositoriesRemoved(List.of(
                new GitHubRepository(1, "username/test", false),
                new GitHubRepository(2, "untitled/23", false),
                new GitHubRepository(3, "untitled/test11", false)));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation_repositories").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(false, integrationRepository.findById(mockIntegration.getId()).isPresent());
        assertEquals(true, integrationRepository.findById(integration.getId()).isPresent());
    }

    @Test
    void githubSuccessDefault() {
        client.post().uri("/webhook/github").header("X-Hub-Signature-256", "sha256=" + utils.hmacHex("{}"))
                .header("X-GitHub-Event", "unknown_event").bodyValue("{}").exchange().expectStatus().isOk();
    }

    @Test
    void githubSuccessIssues() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setAction("opened");
        data.setIssue(new GitHubIssue(1, "url", "open", "title", "body"));
        data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals("title",
                issueRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0).getTask().getName());
        assertEquals(1, issueRepository.findByIssueIdAndGitHubIntegration(1, integration).size());

        data.setAction("labeled");
        data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();

        data.setAction("edited");
        data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals("title 2", issueRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
                .getTask().getName());

        data.setAction("closed");
        data.setIssue(new GitHubIssue(1, "url", "closed", "title 2", "body"));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(2).getId(),
                issueRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
                        .getTask().getStatus().getId());

        data.setAction("reopened");
        data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(0).getId(),
        issueRepository.findByIssueIdAndGitHubIntegration(1, integration).get(0)
                        .getTask().getStatus().getId());

        data.setAction("deleted");
        data.setIssue(new GitHubIssue(1, "url", "open", "title 2", "body"));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(0, issueRepository.findByIssueIdAndGitHubIntegration(1, integration).size());

        data.getRepository().setFullName("username/repo2");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "issues").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(0, issueRepository.findByIssueIdAndGitHubIntegration(1, integration).size());
    }

    @Test
    void githubSuccessPush() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
        data.setCommits(List.of(new GitHubCommit("1", "message without anything interesting")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push").bodyValue(data).exchange().expectStatus().isOk();

        Task task = taskRepository.save(new Task(1, "TEST", "DESC", project.getStatuses().get(0), user, 1));

        data.setCommits(List.of(new GitHubCommit("1", "message without anything interesting"),
                new GitHubCommit("2", "message with something interesting !" + task.getNumber())));

        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(2).getId(),
                taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setCommits(List.of(new GitHubCommit("1", "message without anything interesting"),
                new GitHubCommit("2", "message with something interesting !666")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push").bodyValue(data).exchange().expectStatus().isOk();

        data.setCommits(List.of(new GitHubCommit("1", "message without anything interesting"),
                new GitHubCommit("2", "message with something interesting reopen!" + task.getNumber())));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(0).getId(),
                taskRepository.findById(task.getId()).get().getStatus().getId());

        data.getRepository().setFullName("username/repo2");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "push").bodyValue(data).exchange().expectStatus().isOk();
    }

    @Test
    void githubSuccessInstallation() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setInstallation(new GitHubInstallationApi(2, new GitHubUser(1, "login")));
        data.setAction("unknown");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation").bodyValue(data).exchange().expectStatus().isOk();

        data.setInstallation(new GitHubInstallationApi(1, new GitHubUser(1, "login")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation").bodyValue(data).exchange().expectStatus().isOk();

        data.setAction("suspend");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(true, installationRepository.findByInstallationId(1L).get(0).getSuspended());

        data.setAction("unsuspend");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(false, installationRepository.findByInstallationId(1L).get(0).getSuspended());

        data.setAction("deleted");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "installation").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(false, installationRepository.findById(installation.getId()).isPresent());

        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integration = integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
    }

    @Test
    void githubSuccessPullRequest() throws JsonProcessingException {
        GitHubWebhookData data = new GitHubWebhookData();
        data.setAction("opened");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title", "body", new GitHubBranch("branch")));
        data.setRepository(new GitHubRepository(1, integration.getRepositoryFullName(), false));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();

        Task task = taskRepository.save(new Task(2, "TEST", "DESC", project.getStatuses().get(0), user, 1));
        pullRepository.save(new GitHubTaskPull(task, integration, data.getPullRequest()));

        data.setAction("closed");
        data.setPullRequest(new GitHubPullRequest(20, "url", "closed", "title", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(2).getId(),
                taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setAction("reopened");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals(project.getStatuses().get(0).getId(),
                taskRepository.findById(task.getId()).get().getStatus().getId());

        data.setAction("edited");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title 2", "body", new GitHubBranch("branch")));
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals("TEST", taskRepository.findById(task.getId()).get().getName());

        data.setAction("closed");
        data.setPullRequest(new GitHubPullRequest(20, "url", "open", "title 2", "body", new GitHubBranch("branch")));
        data.getPullRequest().setMerged(true);
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals("merged", taskRepository.findById(task.getId()).get().getPull().getState());

        data.getRepository().setFullName("username/repo2");
        client.post().uri("/webhook/github")
                .header("X-Hub-Signature-256", "sha256=" + utils.hmacHex(MAPPER.writeValueAsString(data)))
                .header("X-GitHub-Event", "pull_request").bodyValue(data).exchange().expectStatus().isOk();
        assertEquals("merged", taskRepository.findById(task.getId()).get().getPull().getState());
    }
}
