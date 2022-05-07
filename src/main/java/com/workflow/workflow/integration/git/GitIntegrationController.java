package com.workflow.workflow.integration.git;

import java.util.List;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.GitHubIntegrationInfo;
import com.workflow.workflow.integration.git.github.service.GitHubService;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

@RestController
public class GitIntegrationController {
    private static final String INTEGRATION_LINK = "https://github.com/apps/workflow-2022/installations/new";
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private GitHubService service;
    @Autowired
    private GitHubInstallationRepository installationRepository;

    @GetMapping("/integration/github")
    Mono<GitHubIntegrationInfo> getRepositories() {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return service.getRepositories(user).map(repos -> new GitHubIntegrationInfo(INTEGRATION_LINK, repos));
    }

    @PostMapping("/integration/github")
    Mono<GitHubIntegrationInfo> postRepositories(long installationId) {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return service.getInstallationUser(installationId)
                .map(gitHubUser -> installationRepository
                        .save(new GitHubInstallation(installationId, user, gitHubUser.getLogin())))
                .map(List::of)
                .flatMap(service::getRepositories)
                .map(repos -> new GitHubIntegrationInfo(INTEGRATION_LINK, repos));
    }

    @GetMapping("/user/integration/github")
    List<GitHubInstallation> getInstallations() {
        // TODO: get current user for now 1
        User user = userRepository.findById(1L).orElseThrow();
        return installationRepository.findByUser(user);
    }
}
