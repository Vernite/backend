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

package dev.vernite.vernite.integration.git.github.entity;

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

import dev.vernite.vernite.integration.git.github.data.GitHubIssue;
import dev.vernite.vernite.integration.git.github.data.GitHubRepository;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskIssue;
import dev.vernite.vernite.integration.git.github.entity.task.GitHubTaskKey;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.user.User;

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

        GitHubTaskIssue gitHubTask = new GitHubTaskIssue(task, gitHubIntegration, new GitHubIssue(0, "url", "state", "title", "body"));

        assertEquals("https://github.com/full/name/issues/0", gitHubTask.getLink());
        gitHubTask.setIssueId(1);
        gitHubTask.setTask(task);
        gitHubTask.setGitHubIntegration(new GitHubIntegration(new Project("2"), null, "full/full"));
        assertEquals("https://github.com/full/full/issues/1", gitHubTask.getLink());

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
