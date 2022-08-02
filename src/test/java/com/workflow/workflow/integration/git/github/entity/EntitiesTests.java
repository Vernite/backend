package com.workflow.workflow.integration.git.github.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.time.Instant;
import java.util.Date;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import com.workflow.workflow.integration.git.github.data.GitHubRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.task.Task;
import com.workflow.workflow.user.User;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class EntitiesTests {
    @Test
    void gitHubTaskTests() {
        Task task = new Task(1, "name", "description", null, null, 0);
        task.setId(1);
        GitHubIntegration gitHubIntegration = new GitHubIntegration(new Project("name"), null, "full/name");
        gitHubIntegration.setId(1);

        GitHubTask gitHubTask = new GitHubTask(task, gitHubIntegration, 0, (byte) 0);

        assertEquals("https://github.com/full/name/issues/0", gitHubTask.getLink());
        gitHubTask.setIssueId(1);
        gitHubTask.setTask(task);
        gitHubTask.setIsPullRequest((byte) 1);
        gitHubTask.setGitHubIntegration(new GitHubIntegration(new Project("2"), null, "full/full"));
        assertEquals("https://github.com/full/full/pull/1", gitHubTask.getLink());

        GitHubTaskKey key = new GitHubTaskKey(new Task(1, "name", "description", null, null, 0), gitHubIntegration);

        assertNotEquals(key, gitHubTask.getId());
        gitHubTask.setId(key);
        assertEquals(key, gitHubTask.getId());

        assertEquals(false, key.equals(null));
        assertEquals(false, key.equals(new Object()));

        GitHubTaskKey other = new GitHubTaskKey(task, new GitHubIntegration(new Project("name"), null, "full/name"));
        
        assertNotEquals(key, other);
        assertNotEquals(key.hashCode(), other.hashCode());

        other.setIntegrationId(666);
        other.setTaskId(key.getTaskId());

        assertNotEquals(key, other);

        other.setIntegrationId(key.getIntegrationId());
        other.setTaskId(666);

        assertNotEquals(key, other);
    }

    @Test
    void gitHubIntegrationTests() {
        GitHubIntegration gitHubIntegration = new GitHubIntegration(new Project("name"), null, "full/name");

        assertEquals("name", gitHubIntegration.getProject().getName());
        gitHubIntegration.setProject(new Project("name2"));
        assertEquals("name2", gitHubIntegration.getProject().getName());

        assertEquals("full/name", gitHubIntegration.getRepositoryFullName());
        gitHubIntegration.setRepositoryFullName("full/name2");
        assertEquals("full/name2", gitHubIntegration.getRepositoryFullName());

        assertEquals(null, gitHubIntegration.getInstallation());
        gitHubIntegration.setInstallation(new GitHubInstallation(1, null, "username"));
        assertEquals(0, gitHubIntegration.getInstallation().getId());
        assertEquals(1, gitHubIntegration.getInstallation().getInstallationId());
        assertEquals("username", gitHubIntegration.getInstallation().getGitHubUsername());
    }

    @Test
    void gitHubInstallationTests() {
        GitHubInstallation gitHubInstallation = new GitHubInstallation(1, null, "username");

        assertEquals(1, gitHubInstallation.getInstallationId());
        gitHubInstallation.setInstallationId(2);
        assertEquals(2, gitHubInstallation.getInstallationId());

        assertEquals("username", gitHubInstallation.getGitHubUsername());
        gitHubInstallation.setGitHubUsername("username2");
        assertEquals("username2", gitHubInstallation.getGitHubUsername());

        assertEquals(null, gitHubInstallation.getToken());
        gitHubInstallation.setToken("token");
        assertEquals("token", gitHubInstallation.getToken());

        assertEquals(true, gitHubInstallation.getExpiresAt().before(new Date()));
        gitHubInstallation.setExpiresAt(Date.from(Instant.now().plusSeconds(1000)));
        assertEquals(false, gitHubInstallation.getExpiresAt().before(new Date()));

        assertEquals(null, gitHubInstallation.getUser());
        gitHubInstallation.setUser(new User("name", "surname", "username", "email", "password"));
        assertEquals("name", gitHubInstallation.getUser().getName());
    }

    @Test
    void gitHubRepositoryTests() {
        GitHubRepository gitHubRepository = new GitHubRepository(1, "full/name", true);
        GitHubRepository other = new GitHubRepository(1, "full/name2", false);

        assertEquals(false, gitHubRepository.equals(null));
        assertEquals(false, gitHubRepository.equals(new Object()));
        assertEquals(false, gitHubRepository.equals(other));
        assertNotEquals(gitHubRepository.hashCode(), other.hashCode());

        gitHubRepository.setFullName(null);
        assertEquals(false, gitHubRepository.equals(other));

        other.setFullName(null);
        assertEquals(false, gitHubRepository.equals(other));

        assertNotEquals(gitHubRepository.hashCode(), other.hashCode());
        
        other.setPrivate(true);

        assertEquals(gitHubRepository, other);
    }
}
