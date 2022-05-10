package com.workflow.workflow.integration.git.github;

import java.util.ArrayList;
import java.util.List;

import com.workflow.workflow.integration.git.github.service.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.service.GitHubRepository;
import com.workflow.workflow.integration.git.github.service.GitHubWebhookData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class GitHubWebhookController {
    @Autowired
    private GitHubTaskRepository taskRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;

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
                taskRepository.deleteAll(taskRepository.findByGitHubIntegration(gitHubIntegration));
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
            taskRepository.deleteAll(taskRepository.findByGitHubIntegration(gitHubIntegration));
        }
        integrationRepository.deleteAll(integrations);
    }

    @PostMapping("/webhook/github")
    void webhook(@RequestBody GitHubWebhookData data) {
        // Delete installation when suspended or deleted
        if (data.getRepositories() != null) {
            handleInstallation(data);
            return;
        }
        // Delete integrations when repositories access is removed
        if (data.getRepositoriesRemoved() != null && !data.getRepositoriesRemoved().isEmpty()) {
            handleInstallationRepositories(data);
            return;
        }
    }
}
