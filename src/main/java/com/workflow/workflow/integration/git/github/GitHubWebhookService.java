package com.workflow.workflow.integration.git.github;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import com.workflow.workflow.integration.git.github.entity.GitHubTask;
import com.workflow.workflow.integration.git.github.entity.GitHubTaskRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;

import org.apache.commons.codec.digest.HmacAlgorithms;
import org.apache.commons.codec.digest.HmacUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class GitHubWebhookService {
    private HmacUtils utils;
    private static final Pattern PATTERN = Pattern.compile("(reopen|close)?!(\\d+)");
    private static final String CLOSED = "closed";
    private static final String EDITED = "edited";
    private final GitHubInstallationRepository installationRepository;
    private final GitHubIntegrationRepository integrationRepository;
    private final TaskRepository taskRepository;
    private final GitHubTaskRepository gitTaskRepository;
    private final GitHubService service;
    private User systemUser;

    public GitHubWebhookService(GitHubInstallationRepository installationRepository,
            GitHubIntegrationRepository integrationRepository, TaskRepository taskRepository,
            GitHubTaskRepository gitHubTaskRepository, GitHubService service, UserRepository userRepository) {
        this.installationRepository = installationRepository;
        this.integrationRepository = integrationRepository;
        this.taskRepository = taskRepository;
        this.gitTaskRepository = gitHubTaskRepository;
        this.service = service;
        this.systemUser = userRepository.findByUsername("Username"); // TODO change system user
        if (this.systemUser == null) {
            this.systemUser = userRepository.save(new User("Name", "Surname", "Username", "wflow1337@gmail.com", "1"));
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
                return handleInstallationRepositories(data);
            case "issues":
                return handleIssue(data);
            case "push":
                return handlePush(data);
            case "installation":
                return handleInstallation(data);
            case "pull_request":
                return handlePullRequest(data);
            default:
                return Mono.empty();
        }
    }

    private Mono<Void> handleInstallation(GitHubWebhookData data) {
        GitHubInstallationApi installationApi = data.getInstallation();
        Optional<GitHubInstallation> optional = installationRepository.findByInstallationId(installationApi.getId());
        if (optional.isEmpty()) {
            return Mono.empty();
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
        return Mono.empty();
    }

    private Mono<Void> handlePush(GitHubWebhookData data) {
        List<Task> tasks = new ArrayList<>();
        for (GitHubCommit commit : data.getCommits()) {
            Matcher matcher = PATTERN.matcher(commit.getMessage());
            if (matcher.find()) {
                String controlKeyword = matcher.group(1);
                Optional<Task> optional = taskRepository.findByIdAndActiveNull(Long.parseLong(matcher.group(2)));
                if (optional.isEmpty()) {
                    continue;
                }
                Task task = optional.get();
                for (GitHubIntegration integration : integrationRepository
                        .findByRepositoryFullName(data.getRepository().getFullName())) {
                    if (task.getStatus().getProject().getId() == integration.getProject().getId()) {
                        task.setState("reopen".equals(controlKeyword) ? "open" : CLOSED);
                        tasks.add(taskRepository.save(task));
                    }
                }
            }
        }
        return Flux.fromIterable(tasks).flatMap(service::patchIssue).then();
    }

    private Mono<Void> handleIssue(GitHubWebhookData data) {
        GitHubRepository repository = data.getRepository();
        GitHubIssue issue = data.getIssue();
        for (GitHubIntegration gitHubIntegration : integrationRepository
                .findByRepositoryFullName(repository.getFullName())) {
            if (data.getAction().equals("opened") && gitTaskRepository
                    .findByIssueIdAndGitHubIntegration(issue.getNumber(), gitHubIntegration).isEmpty()) {
                Task task = new Task();
                task.setUser(systemUser);
                task.setCreatedAt(new Date());
                task.setDeadline(new Date());
                task.setDescription(issue.getBody());
                task.setName(issue.getTitle());
                task.setStatus(gitHubIntegration.getProject().getStatuses().stream().findFirst().orElseThrow());
                task.setState("open");
                task.setType(0);
                task = taskRepository.save(task);
                gitTaskRepository.save(new GitHubTask(task, gitHubIntegration, issue.getNumber(), (byte) 0));
            } else {
                for (GitHubTask gitHubTask : gitTaskRepository.findByIssueIdAndGitHubIntegration(issue.getNumber(),
                        gitHubIntegration)) {
                    Task task = gitHubTask.getTask();
                    if (data.getAction().equals(EDITED)) {
                        task.setDescription(issue.getBody());
                        task.setName(issue.getTitle());
                        taskRepository.save(task);
                    } else if (data.getAction().equals(CLOSED) || data.getAction().equals("reopened")) {
                        task.setState(issue.getState());
                        taskRepository.save(task);
                    } else if (data.getAction().equals("deleted")) {
                        gitTaskRepository.delete(gitHubTask);
                        taskRepository.delete(task);
                    }

                }
            }
        }
        return Mono.empty();
    }

    private Mono<Void> handleInstallationRepositories(GitHubWebhookData data) {
        if (data.getRepositoriesRemoved() == null) {
            return Mono.empty();
        }
        List<GitHubIntegration> integrations = new ArrayList<>();
        for (GitHubRepository repository : data.getRepositoriesRemoved()) {
            integrations.addAll(integrationRepository.findByRepositoryFullName(repository.getFullName()));
        }
        integrationRepository.deleteAll(integrations);
        return Mono.empty();
    }

    private Mono<Void> handlePullRequest(GitHubWebhookData data) {
        GitHubPullRequest pullRequest = data.getPullRequest();
        GitHubRepository repository = data.getRepository();
        if (data.getAction().equals(CLOSED) || data.getAction().equals("reopened")) {
            for (GitHubIntegration gitHubIntegration : integrationRepository
                    .findByRepositoryFullName(repository.getFullName())) {
                for (GitHubTask gitHubTask : gitTaskRepository.findByIssueIdAndGitHubIntegration(
                        pullRequest.getNumber(),
                        gitHubIntegration)) {
                    if (pullRequest.isMerged()) {
                        gitHubTask.setIsPullRequest((byte) 2);
                        gitTaskRepository.save(gitHubTask);
                    }
                    Task task = gitHubTask.getTask();
                    task.setState(pullRequest.getState());
                    taskRepository.save(task);
                }
            }
        }
        return Mono.empty();
    }
}
