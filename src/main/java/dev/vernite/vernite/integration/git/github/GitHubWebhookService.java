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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

import dev.vernite.vernite.common.utils.counter.CounterSequenceRepository;
import dev.vernite.vernite.integration.git.github.data.GitHubInstallationApi;
import dev.vernite.vernite.integration.git.github.data.GitHubRepository;
import dev.vernite.vernite.integration.git.github.data.GitHubWebhookData;
import dev.vernite.vernite.integration.git.github.model.AuthorizationRepository;
import dev.vernite.vernite.integration.git.github.model.CommentIntegration;
import dev.vernite.vernite.integration.git.github.model.CommentIntegrationRepository;
import dev.vernite.vernite.integration.git.github.model.Installation;
import dev.vernite.vernite.integration.git.github.model.InstallationRepository;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegration;
import dev.vernite.vernite.integration.git.github.model.ProjectIntegrationRepository;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationRepository;
import dev.vernite.vernite.integration.git.github.model.TaskIntegration;
import dev.vernite.vernite.integration.git.github.model.TaskIntegrationId;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
import dev.vernite.vernite.task.comment.Comment;
import dev.vernite.vernite.task.comment.CommentRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
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
    private InstallationRepository installationRepository;
    @Autowired
    private ProjectIntegrationRepository integrationRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private TaskIntegrationRepository issueRepository;
    @Autowired
    private GitHubService service;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;
    @Autowired
    private AuthorizationRepository authorizationRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private CommentIntegrationRepository commentIntegrationRepository;
    private User systemUser;

    @Autowired
    public void setSystemUser(UserRepository userRepository) {
        systemUser = userRepository.findByUsername("Username"); // TODO change system user
        if (systemUser == null) {
            systemUser = userRepository.save(new User("Name", "Surname", "Username", "contact@vernite.dev", "1"));
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
            case "issue_comment":
                handleIssueComment(data);
                break;
            default:
                break;
        }
        return Mono.empty();
    }

    private void handleIssueComment(GitHubWebhookData data) {
        switch (data.getAction()) {
            case "created":
                var name = data.getRepository().getFullName().split("/");
                if (commentIntegrationRepository.findById(data.getComment().getId()).isPresent()) {
                    return;
                }
                integrationRepository.findByRepositoryOwnerAndRepositoryName(name[0], name[1])
                        .forEach(projectIntegration -> {
                            var issues = issueRepository.findByProjectIntegrationAndIssueId(projectIntegration,
                                    data.getIssue().getNumber());
                            issues.forEach(issue -> {
                                var task = issue.getTask();
                                var comment = new Comment(task, data.getComment().getBody(), systemUser);
                                commentRepository.save(comment);
                                var commentIntegration = new CommentIntegration(data.getComment().getId(), comment);
                                commentIntegrationRepository.save(commentIntegration);
                            });
                        });

                break;
            case "edited":
                var integration = commentIntegrationRepository.findById(data.getComment().getId());
                if (integration.isEmpty()) {
                    return;
                }
                var commentIntegration = integration.get();
                var commentEntity = commentIntegration.getComment();
                commentEntity.setContent(data.getComment().getBody());
                commentRepository.save(commentEntity);
                break;
            case "deleted":
                integration = commentIntegrationRepository.findById(data.getComment().getId());
                if (integration.isEmpty()) {
                    return;
                }
                commentIntegration = integration.get();
                commentRepository.delete(commentIntegration.getComment());
                break;
            default:
                break;
        }
    }

    private void handleInstallation(GitHubWebhookData data) {
        GitHubInstallationApi installationApi = data.getInstallation();
        Optional<Installation> optional = installationRepository.findById(installationApi.getId());
        if (optional.isEmpty()) {
            return;
        }
        var installation = optional.get();
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
        var repository = data.getRepository();
        List<Task> tasks = new ArrayList<>();
        var name = repository.getFullName().split("/");
        for (var integration : integrationRepository.findByRepositoryOwnerAndRepositoryName(name[0], name[1])) {
            for (var commit : data.getCommits()) {
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
        var repository = data.getRepository();
        var issue = data.getIssue();
        var name = repository.getFullName().split("/");
        for (var integration : integrationRepository.findByRepositoryOwnerAndRepositoryName(name[0], name[1])) {
            if (data.getAction().equals("opened")
                    && issueRepository.findByProjectIntegrationAndIssueId(integration, issue.getNumber()).isEmpty()) {
                long id = counterSequenceRepository
                        .getIncrementCounter(integration.getProject().getTaskCounter().getId());
                Status status = integration.getProject().getStatuses().get(0);
                Task task = new Task(id, issue.getTitle(), issue.getBody(), status, systemUser, 0);
                task.changeStatus(true);
                task = taskRepository.save(task);
                issueRepository
                        .save(new TaskIntegration(task, integration, issue.getNumber(), TaskIntegration.Type.ISSUE));
            } else {
                for (var gitTask : issueRepository.findByProjectIntegrationAndIssueId(integration, issue.getNumber())) {
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
                            authorizationRepository.findById(data.getAssignee().getId())
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
        List<ProjectIntegration> integrations = new ArrayList<>();
        for (GitHubRepository repository : data.getRepositoriesRemoved()) {
            var name = repository.getFullName().split("/");
            integrations.addAll(integrationRepository.findByRepositoryOwnerAndRepositoryName(name[0], name[1]));
        }
        integrationRepository.deleteAll(integrations);
    }

    private void handlePullRequest(GitHubWebhookData data) {
        var pullRequest = data.getPullRequest();
        GitHubRepository repository = data.getRepository();
        var name = repository.getFullName().split("/");
        for (var integration : integrationRepository.findByRepositoryOwnerAndRepositoryName(name[0], name[1])) {
            for (var gitTask : issueRepository.findByProjectIntegrationAndIssueId(integration,
                    pullRequest.getNumber())) {
                Task task = gitTask.getTask();
                switch (data.getAction()) {
                    case CLOSED:
                    case "reopened":
                        if (pullRequest.isMerged()) {
                            gitTask.setMerged(true);
                            issueRepository.save(gitTask);
                        }
                        task.changeStatus(pullRequest.getState().equals("open"));
                        taskRepository.save(task);
                        break;
                    case "assigned":
                        authorizationRepository.findById(data.getAssignee().getId())
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
                        task.setName(pullRequest.getTitle());
                        task.setDescription(pullRequest.getBody());
                        taskRepository.save(task);
                        issueRepository.save(gitTask);
                        break;
                    default:
                        break;
                }
            }
        }
    }
}
