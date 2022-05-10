package com.workflow.workflow.integration.git.github;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.workflow.workflow.integration.git.github.service.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.service.GitHubIssue;
import com.workflow.workflow.integration.git.github.service.GitHubRepository;
import com.workflow.workflow.integration.git.github.service.GitHubWebhookData;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
            if (data.getAction().equals("opened")) {
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

    @PostMapping("/webhook/github")
    void webhook(@RequestBody GitHubWebhookData data) {
        // Delete integrations when repositories access is removed
        if (data.getRepositoriesRemoved() != null && !data.getRepositoriesRemoved().isEmpty()) {
            handleInstallationRepositories(data);
            return;
        }
        // Handle issue changes
        if (data.getIssue() != null) {
            handleIssue(data);
            return;
        }
        // Delete installation when suspended or deleted
        handleInstallation(data);
    }
}
