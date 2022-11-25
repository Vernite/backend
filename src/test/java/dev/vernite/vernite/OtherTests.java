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

package dev.vernite.vernite;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;

import dev.vernite.vernite.counter.CounterSequence;
import dev.vernite.vernite.integration.git.github.entity.GitHubIntegration;
import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectMember;
import dev.vernite.vernite.projectworkspace.ProjectWithPrivileges;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceKey;
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class OtherTests {
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    private User user;
    private UserSession session;
    private Workspace workspace;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_projects_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
    }

    @BeforeEach
    void reset() {
        projectRepository.deleteAll();
    }

    @Test
    void projectTests() {
        Project project = new Project("name");

        project.setGitHubIntegration(new GitHubIntegration(project, null, "repository/name"));
        assertEquals("repository/name", project.getGitHubIntegration());
        project.setSprintCounter(new CounterSequence());
        assertEquals(0, project.getSprintCounter().getCounterValue());
        project.setStatusCounter(new CounterSequence());
        assertEquals(0, project.getStatusCounter().getCounterValue());
        project.setTaskCounter(new CounterSequence());
        assertEquals(0, project.getTaskCounter().getCounterValue());
        assertEquals(3, project.getStatuses().size());
        project.setStatuses(List.of(new Status()));
        assertEquals(1, project.getStatuses().size());
        project.setProjectWorkspaces(List.of(new ProjectWorkspace()));
        assertEquals(1, project.getProjectWorkspaces().size());
    }

    @Test
    void projectMemberTests() {
        ProjectMember member = new ProjectMember(user, 1L);
        ProjectMember other = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(null));
        assertEquals(false, member.equals((Object) "name"));
        assertEquals(true, member.equals(other));

        other = new ProjectMember(user, 2L);

        assertEquals(false, member.equals(other));

        other = new ProjectMember(null, 1L);

        assertEquals(false, member.equals(other));

        member = new ProjectMember(null, 1L);

        assertEquals(true, member.equals(other));

        other = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(other));

        User otherUser = new User();
        otherUser.setId(666L);
        other = new ProjectMember(otherUser, 1L);

        member = new ProjectMember(user, 1L);

        assertEquals(false, member.equals(other));
    }

    @Test
    void projectWithPrivilegesTests() {
        Project project = new Project("name");
        Project other = new Project("other");

        ProjectWithPrivileges pwp = new ProjectWithPrivileges(project, 1L);
        ProjectWithPrivileges opp = new ProjectWithPrivileges(other, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals((Object) "name"));
        assertEquals(false, pwp.equals(opp));
        assertEquals(true, pwp.compareTo(opp) < 0);
        assertNotEquals(pwp.hashCode(), opp.hashCode());

        pwp = new ProjectWithPrivileges(null, 1L);

        assertEquals(false, pwp.equals(opp));
        assertNotEquals(pwp.hashCode(), opp.hashCode());

        opp = new ProjectWithPrivileges(null, 1L);

        assertEquals(true, pwp.equals(opp));

        opp = new ProjectWithPrivileges(project, 2L);

        assertEquals(false, pwp.equals(opp));
    }

    @Test
    void projectWorkspaceTests() {
        Project project = new Project("name");
        Project other = new Project("other");
        other.setId(333L);

        ProjectWorkspace pwp = new ProjectWorkspace(project, workspace, 1L);
        ProjectWorkspace opp = new ProjectWorkspace(other, workspace, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals((Object) "name"));
        assertEquals(false, pwp.equals(opp));
        assertNotEquals(pwp.hashCode(), opp.hashCode());

        pwp.setId(null);

        assertEquals(false, pwp.equals(opp));
        assertNotEquals(pwp.hashCode(), opp.hashCode());

        opp.setId(null);

        assertEquals(true, pwp.equals(opp));

        opp.setPrivileges(2L);

        assertEquals(false, pwp.equals(opp));

        opp.setProject(null);
        opp.setWorkspace(null);

        assertEquals(false, pwp.equals(opp));

        ProjectWorkspaceKey key = new ProjectWorkspaceKey(workspace, project);

        assertEquals(false, key.equals(null));
        assertEquals(false, key.equals((Object) "name"));
        assertEquals(0, key.compareTo(key));
        assertEquals(true, key.compareTo(new ProjectWorkspaceKey(workspace, other)) < 0);

        assertEquals(project.getId(), key.getProjectId());
        assertEquals(workspace.getId(), key.getWorkspaceId());

        key.setWorkspaceId(null);
        key.setProjectId(0);

        assertEquals(null, key.getWorkspaceId());
        assertEquals(0, key.getProjectId());

        assertNotEquals(new ProjectWorkspaceKey(workspace, project).hashCode(), key.hashCode());
        assertNotEquals(new ProjectWorkspaceKey(workspace, project), key);
        assertNotEquals(key, new ProjectWorkspaceKey(workspace, project));
        assertEquals(true, key.equals(key));
    }
}
