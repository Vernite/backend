package com.workflow.workflow.integration.git.github;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.github.service.GitHubCommit;
import com.workflow.workflow.integration.git.github.service.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.service.GitHubIssue;
import com.workflow.workflow.integration.git.github.service.GitHubRepository;
import com.workflow.workflow.integration.git.github.service.GitHubWebhookData;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.UserRepository;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GitHubWebhookController {
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private GitHubTaskRepository gitTaskRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private UserRepository userRepository;
    private static final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final HmacUtils utils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256,
            "5CxrXejuNwslaS2iIm0ELDry313vqwC3ZdmAId3CqFc5L6GH");

    void handleInstallation(GitHubWebhookData data) {
        GitHubInstallationApi installationApi = data.getInstallation();
        GitHubInstallation installation = installationRepository.findByInstallationId(installationApi.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.OK));
        if (data.getAction().equals("suspend")) {
            installation.setSuspended(true);
            installationRepository.save(installation);
        }
        if (data.getAction().equals("unsuspend")) {
            installation.setSuspended(false);
            installationRepository.save(installation);
        }
        if (data.getAction().equals("deleted")) {
            List<GitHubIntegration> integrations = integrationRepository.findByInstallation(installation);
            for (GitHubIntegration gitHubIntegration : integrations) {
                gitTaskRepository.deleteAll(gitTaskRepository.findByGitHubIntegration(gitHubIntegration));
            }
            integrationRepository.deleteAll(integrations);
            installationRepository.delete(installation);
        }
    }

    void handleInstallationRepositories(GitHubWebhookData data) {
        List<GitHubIntegration> integrations = new ArrayList<>();
        for (GitHubRepository gitHubRepository : data.getRepositoriesRemoved()) {
            integrations.addAll(integrationRepository.findByRepositoryFullName(gitHubRepository.getFullName()));
        }
        for (GitHubIntegration gitHubIntegration : integrations) {
            gitTaskRepository.deleteAll(gitTaskRepository.findByGitHubIntegration(gitHubIntegration));
        }
        integrationRepository.deleteAll(integrations);
    }

    void handleIssue(GitHubWebhookData data) {
        GitHubRepository repository = data.getRepository();
        GitHubIssue issue = data.getIssue();
        for (GitHubIntegration gitHubIntegration : integrationRepository
                .findByRepositoryFullName(repository.getFullName())) {
            if (data.getAction().equals("opened") && gitTaskRepository
                    .findByIssueIdAndGitHubIntegration(issue.getNumber(), gitHubIntegration).isEmpty()) {
                Task task = new Task();
                task.setUser(userRepository.findById(1L).orElseThrow()); // TODO: change to auto user
                task.setCreatedAt(new Date());
                task.setDeadline(new Date());
                task.setDescription(issue.getBody());
                task.setName(issue.getTitle());
                task.setStatus(gitHubIntegration.getProject().getStatuses().stream().findFirst().orElseThrow());
                task.setState("open");
                task.setType(0);
                task = taskRepository.save(task);
                gitTaskRepository.save(new GitHubTask(task, gitHubIntegration, issue.getNumber()));
            } else {
                for (GitHubTask gitHubTask : gitTaskRepository.findByIssueIdAndGitHubIntegration(issue.getNumber(),
                        gitHubIntegration)) {
                    Task task = gitHubTask.getTask();
                    if (data.getAction().equals("edited")) {
                        task.setDescription(issue.getBody());
                        task.setName(issue.getTitle());
                        taskRepository.save(task);
                    } else if (data.getAction().equals("closed") || data.getAction().equals("reopened")) {
                        task.setState(issue.getState());
                        taskRepository.save(task);
                    } else if (data.getAction().equals("deleted")) {
                        gitTaskRepository.delete(gitHubTask);
                        taskRepository.delete(task);
                    }

                }
            }
        }
    }

    void handlePush(GitHubWebhookData data) {
        for (GitHubCommit gitHubCommit : data.getCommits()) {
            if (gitHubCommit.getId().equals(data.getAfter())) {
                Pattern pattern = Pattern.compile("@(\\d+)");
                String message = gitHubCommit.getMessage();
                Matcher matcher = pattern.matcher(message);
                if  (matcher.find()) {
                    long taskId = Long.parseLong(matcher.group(1));
                    GitHubRepository repository = data.getRepository();
                    Task task = taskRepository.findById(taskId).orElseThrow(() -> new ResponseStatusException(HttpStatus.OK));
                    for (GitHubIntegration gitHubIntegration : integrationRepository.findByRepositoryFullName(repository.getFullName())) {
                        if (task.getStatus().getProject().getId().equals(gitHubIntegration.getProject().getId())) {
                            task.setState("closed");
                            taskRepository.save(task);
                            return;
                        }
                    }
                }
            }
        }
    }

    @PostMapping("/webhook/github")
    void webhook(@RequestHeader("X-Hub-Signature-256") String token, @RequestHeader("X-GitHub-Event") String event,
            @RequestBody String dataRaw) {
        if (!MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                ("sha256=" + utils.hmacHex(dataRaw)).getBytes(StandardCharsets.UTF_8))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        GitHubWebhookData data;
        try {
            data = mapper.readValue(dataRaw, GitHubWebhookData.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        // Delete integrations when repositories access is removed
        if (event.equals("installation_repositories") && !data.getRepositoriesRemoved().isEmpty()) {
            handleInstallationRepositories(data);
            return;
        }
        // Handle issue changes
        if (event.equals("issues")) {
            handleIssue(data);
            return;
        }
        // Handle commit comment
        if (event.equals("push")) {
            handlePush(data);
            return;
        }
        // Delete installation when suspended or deleted
        if (event.equals("installation")) {
            handleInstallation(data);
        }
    }
}
