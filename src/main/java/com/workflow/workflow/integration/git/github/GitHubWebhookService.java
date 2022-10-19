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

package com.workflow.workflow.integration.git.github;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.integration.git.github.data.GitHubCommit;
import com.workflow.workflow.integration.git.github.data.GitHubInstallationApi;
import com.workflow.workflow.integration.git.github.data.GitHubIssue;
import com.workflow.workflow.integration.git.github.data.GitHubPullRequest;
import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.integration.git.github.data.GitHubWebhookData;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.integration.git.github.entity.task.GitHubTaskIssue;
import com.workflow.workflow.integration.git.github.entity.task.GitHubTaskIssueRepository;
import com.workflow.workflow.integration.git.github.entity.task.GitHubTaskPull;
import com.workflow.workflow.integration.git.github.entity.task.GitHubTaskPullRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Component
public class GitHubWebhookService {
    private HmacUtils utils;
    private static final Pattern PATTERN = Pattern.compile("(reopen|close)?!(\\d+)");
    private static final String CLOSED = "closed";
    private static final String EDITED = "edited";
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
    @Autowired
    private GitHubService service;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;
    private User systemUser;

    @Autowired
    public void setSystemUser(UserRepository userRepository) {
        systemUser = userRepository.findByUsername("Username"); // TODO change system user
        if (systemUser == null) {
            systemUser = userRepository.save(new User("Name", "Surname", "Username", "wflow1337@gmail.com", "1"));
        }
    }

    @Value("${githubKey}")
    private void loadHmacUtils(String githubKey) {
        utils = new HmacUtils(HmacAlgorithms.HMAC_SHA_256, githubKey);
    }

    public boolean isAuthorized(String token, String dataRaw) {
        return MessageDigest.isEqual(token.getBytes(StandardCharsets.UTF_8),
                ("sha256=" + utils.hmacHex(dataRaw)).getBytes(StandardCharsets.UTF_8));
    }

    public Mono<Void> handleWebhook(String event, GitHubWebhookData data) {
        switch (event) {
            case "installation_repositories":
                handleInstallationRepositories(data);
                break;
            case "issues":
                handleIssue(data);
                break;
            case "push":
                return handlePush(data);
            case "installation":
                handleInstallation(data);
                break;
            case "pull_request":
                handlePullRequest(data);
                break;
            default:
                break;
        }
        return Mono.empty();
    }

    private void handleInstallation(GitHubWebhookData data) {
        GitHubInstallationApi installationApi = data.getInstallation();
        Optional<GitHubInstallation> optional = installationRepository.findByInstallationId(installationApi.getId());
        if (optional.isEmpty()) {
            return;
        }
        GitHubInstallation installation = optional.get();
        switch (data.getAction()) {
            case "suspend":
                installation.setSuspended(true);
                installationRepository.save(installation);
                break;
            case "unsuspend":
                installation.setSuspended(false);
                installationRepository.save(installation);
                break;
            case "deleted":
                installationRepository.delete(installation);
                break;
            default:
                break;
        }
    }

    private Mono<Void> handlePush(GitHubWebhookData data) {
        GitHubRepository repository = data.getRepository();
        List<Task> tasks = new ArrayList<>();
        for (GitHubIntegration integration : integrationRepository.findByRepositoryFullName(repository.getFullName())) {
            for (GitHubCommit commit : data.getCommits()) {
                Matcher matcher = PATTERN.matcher(commit.getMessage());
                if (matcher.find()) {
                    boolean isOpen = "reopen".equals(matcher.group(1));
                    long taskId = Long.parseLong(matcher.group(2));
                    taskRepository.findByStatusProjectAndNumberAndActiveNull(integration.getProject(), taskId)
                            .ifPresent(task -> {
                                task.changeStatus(isOpen);
                                tasks.add(taskRepository.save(task));
                            });

                }
            }
        }
        return Flux.fromIterable(tasks).flatMap(service::patchIssue).then();
    }

    private void handleIssue(GitHubWebhookData data) {
        GitHubRepository repository = data.getRepository();
        GitHubIssue issue = data.getIssue();
        for (GitHubIntegration integration : integrationRepository.findByRepositoryFullName(repository.getFullName())) {
            if (data.getAction().equals("opened")
                    && issueRepository.findByIssueIdAndGitHubIntegration(issue.getNumber(), integration).isEmpty()) {
                long id = counterSequenceRepository
                        .getIncrementCounter(integration.getProject().getTaskCounter().getId());
                Status status = integration.getProject().getStatuses().get(0);
                Task task = new Task(id, issue.getTitle(), issue.getBody(), status, systemUser, 0);
                task.changeStatus(true);
                task = taskRepository.save(task);
                issueRepository.save(new GitHubTaskIssue(task, integration, issue));
            } else {
                for (GitHubTaskIssue gitTask : issueRepository.findByIssueIdAndGitHubIntegration(issue.getNumber(),
                        integration)) {
                    Task task = gitTask.getTask();
                    switch (data.getAction()) {
                        case EDITED:
                            task.setName(issue.getTitle());
                            task.setDescription(issue.getBody());
                            taskRepository.save(task);
                            issueRepository.save(gitTask);
                            break;
                        case CLOSED:
                            task.changeStatus(false);
                            taskRepository.save(task);
                            break;
                        case "reopened":
                            task.changeStatus(true);
                            taskRepository.save(task);
                            break;
                        case "deleted":
                            issueRepository.delete(gitTask);
                            taskRepository.delete(task);
                            break;
                        case "assigned":
                            installationRepository.findByGitHubUsername(data.getAssignee().getLogin())
                                    .ifPresent(installation -> {
                                        if (integration.getProject().member(installation.getUser()) != -1) {
                                            task.setAssignee(installation.getUser());
                                            taskRepository.save(task);
                                        }
                                    });
                            break;
                        case "unassigned":
                            task.setAssignee(null);
                            taskRepository.save(task);
                            break;
                        default:
                            break;
                    }
                }
            }
        }
    }

    private void handleInstallationRepositories(GitHubWebhookData data) {
        if (data.getRepositoriesRemoved() == null) {
            return;
        }
        List<GitHubIntegration> integrations = new ArrayList<>();
        for (GitHubRepository repository : data.getRepositoriesRemoved()) {
            integrations.addAll(integrationRepository.findByRepositoryFullName(repository.getFullName()));
        }
        integrationRepository.deleteAll(integrations);
    }

    private void handlePullRequest(GitHubWebhookData data) {
        GitHubPullRequest pullRequest = data.getPullRequest();
        GitHubRepository repository = data.getRepository();
        for (GitHubIntegration integration : integrationRepository.findByRepositoryFullName(repository.getFullName())) {
            for (GitHubTaskPull gitTask : pullRepository.findByIssueIdAndGitHubIntegration(pullRequest.getNumber(),
                    integration)) {
                Task task = gitTask.getTask();
                switch (data.getAction()) {
                    case CLOSED:
                    case "reopened":
                        if (pullRequest.isMerged()) {
                            gitTask.setMerged(true);
                            pullRepository.save(gitTask);
                        }
                        task.changeStatus(pullRequest.getState().equals("open"));
                        taskRepository.save(task);
                        break;
                    case "assigned":
                        installationRepository.findByGitHubUsername(data.getAssignee().getLogin())
                                .ifPresent(installation -> {
                                    if (integration.getProject().member(installation.getUser()) != -1) {
                                        task.setAssignee(installation.getUser());
                                        taskRepository.save(task);
                                    }
                                });
                        break;
                    case "unassigned":
                        task.setAssignee(null);
                        taskRepository.save(task);
                        break;
                    case EDITED:
                        gitTask.setTitle(pullRequest.getTitle());
                        gitTask.setDescription(pullRequest.getBody());
                        pullRepository.save(gitTask);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
