package com.workflow.workflow.integration.git;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.workflow.workflow.integration.git.github.entity.GitHubInstallation;
import com.workflow.workflow.integration.git.github.entity.GitHubInstallationRepository;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegrationRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.status.StatusRepository;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.task.TaskRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class GitTaskServiceTests {
    @Autowired
    private GitTaskService service;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private GitHubInstallationRepository installationRepository;
    @Autowired
    private GitHubIntegrationRepository integrationRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private TaskRepository taskRepository;

    private User user;
    private Project project;
    private Status[] statuses = new Status[2];
    private GitHubInstallation installation;
    private Workspace workspace;

    @BeforeAll
    public void init() {
        integrationRepository.deleteAll();
        installationRepository.deleteAll();
        user = userRepository.findById(1L)
                .orElseGet(() -> userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1")));
        project = projectRepository.save(new Project("NAME"));
        statuses[0] = statusRepository.save(new Status("NAME", 1, false, true, 0, project));
        statuses[1] = statusRepository.save(new Status("NAME", 1, true, false, 1, project));
        installation = installationRepository.save(new GitHubInstallation(1, user, "username"));
        integrationRepository.save(new GitHubIntegration(project, installation, "username/repo"));
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @Test
    void connectIssueTest() {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        Issue issue = new Issue(1, "url", "title", "description", "unknown");

        Issue result = service.connectIssue(task, issue).block();
        assertEquals(null, result);
    }

    @Test
    void connectPullRequestTest() {
        Task task = taskRepository.save(new Task("name", "description", statuses[0], user, 0));
        PullRequest pull = new PullRequest(1, "url", "title", "description", "unknown", "branch");

        PullRequest result = service.connectPullRequest(task, pull).block();
        assertEquals(null, result);
    }
}
