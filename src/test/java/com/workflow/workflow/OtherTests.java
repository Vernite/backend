package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;

import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.integration.git.github.entity.GitHubIntegration;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectMember;
import com.workflow.workflow.projectworkspace.ProjectWithPrivileges;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceKey;
import com.workflow.workflow.status.Status;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceRepository;

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
        workspace = workspaceRepository.save(new Workspace(1, user, "Project Tests"));
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
    void projectWithPrivillagesTests() {
        Project project = new Project("name");
        Project other = new Project("other");

        ProjectWithPrivileges pwp = new ProjectWithPrivileges(project, 1L);
        ProjectWithPrivileges opwp = new ProjectWithPrivileges(other, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals((Object) "name"));
        assertEquals(false, pwp.equals(opwp));
        assertEquals(true, pwp.compareTo(opwp) < 0);
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        pwp = new ProjectWithPrivileges(null, 1L);

        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        opwp = new ProjectWithPrivileges(null, 1L);

        assertEquals(true, pwp.equals(opwp));

        opwp = new ProjectWithPrivileges(project, 2L);

        assertEquals(false, pwp.equals(opwp));
    }

    @Test
    void projectWorkspaceTests() {
        Project project = new Project("name");
        Project other = new Project("other");
        other.setId(333L);

        ProjectWorkspace pwp = new ProjectWorkspace(project, workspace, 1L);
        ProjectWorkspace opwp = new ProjectWorkspace(other, workspace, 1L);

        assertEquals(false, pwp.equals(null));
        assertEquals(false, pwp.equals((Object) "name"));
        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        pwp.setId(null);

        assertEquals(false, pwp.equals(opwp));
        assertNotEquals(pwp.hashCode(), opwp.hashCode());

        opwp.setId(null);

        assertEquals(true, pwp.equals(opwp));

        opwp.setPrivileges(2L);

        assertEquals(false, pwp.equals(opwp));

        opwp.setProject(null);
        opwp.setWorkspace(null);

        assertEquals(false, pwp.equals(opwp));

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
