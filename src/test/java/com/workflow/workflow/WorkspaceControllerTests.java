package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.counter.CounterSequenceRepository;
import com.workflow.workflow.project.Project;
import com.workflow.workflow.project.ProjectRepository;
import com.workflow.workflow.projectworkspace.ProjectWorkspace;
import com.workflow.workflow.projectworkspace.ProjectWorkspaceRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.workspace.Workspace;
import com.workflow.workflow.workspace.WorkspaceKey;
import com.workflow.workflow.workspace.WorkspaceRepository;
import com.workflow.workflow.workspace.WorkspaceRequest;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource("classpath:application-test.properties")
public class WorkspaceControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private CounterSequenceRepository counterSequenceRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    private User user;
    private UserSession session;

    @BeforeAll
    void init() {
        user = userRepository.findById(1L).orElseGet(() -> {
            CounterSequence counterSequence = new CounterSequence();
            counterSequence = counterSequenceRepository.save(counterSequence);
            return userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1", counterSequence));
        });
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_workspace_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_workspace_tests").orElseThrow();
        }
    }

    @BeforeEach
    void reset() {
        workspaceRepository.deleteAll();
    }

    @Test
    void getAllWorkspacesSuccess() {
        // Test empty return list
        client.get().uri("/workspace")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Workspace.class).hasSize(0);
        // Prepare some workspaces for next test
        List<Workspace> workspaces = List.of(
                workspaceRepository.save(new Workspace(1, user, "Test 1")),
                workspaceRepository.save(new Workspace(2, user, "Test 3")),
                workspaceRepository.save(new Workspace(3, user, "Test 2")));
        // Test non empty return list
        List<Workspace> result = client.get().uri("/workspace")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Workspace.class)
                .hasSize(3)
                .returnResult()
                .getResponseBody();
        assertEquals(workspaces.get(0).getId().getId(), result.get(0).getId().getId());
        assertEquals(workspaces.get(0).getName(), result.get(0).getName());
        assertEquals(workspaces.get(1).getId().getId(), result.get(2).getId().getId());
        assertEquals(workspaces.get(1).getName(), result.get(2).getName());
        assertEquals(workspaces.get(2).getId().getId(), result.get(1).getId().getId());
        assertEquals(workspaces.get(2).getName(), result.get(1).getName());

        workspaces.get(0).setActive(new Date());
        workspaceRepository.save(workspaces.get(0));

        client.get().uri("/workspace")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(Workspace.class)
                .hasSize(2);
    }

    @Test
    void getAllWorkspacesUnauthorized() {
        client.get().uri("/workspace")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void newWorkspaceSuccess() {
        WorkspaceRequest request = new WorkspaceRequest("POST");
        Workspace workspace = client.post().uri("/workspace")
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Workspace.class)
                .returnResult()
                .getResponseBody();
        Optional<Workspace> optional = workspaceRepository.findById(new WorkspaceKey(workspace.getId().getId(), user));
        assertEquals(true, optional.isPresent());
        Workspace result = optional.get();
        assertEquals(workspace.getName(), result.getName());
        assertEquals(workspace.getId().getId(), result.getId().getId());
    }

    @Test
    void newWorkspaceBadRequest() {
        WorkspaceRequest request = new WorkspaceRequest();
        client.post().uri("/workspace")
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();

        request.setName("0".repeat(51));
        client.post().uri("/workspace")
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void newWorkspaceUnauthorized() {
        client.post().uri("/workspace")
                .contentType(MediaType.APPLICATION_JSON)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getWorkspaceSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "GET"));

        Workspace result = client.get().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk()
                .expectBody(Workspace.class)
                .returnResult()
                .getResponseBody();

        assertEquals(workspace.getId().getId(), result.getId().getId());
        assertEquals(workspace.getName(), result.getName());
    }

    @Test
    void getWorkspaceUnathorized() {
        client.get().uri("/workspace/1")
                .exchange()
                .expectStatus().isUnauthorized();

        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "GET"));
        client.get().uri("/workspace/" + workspace.getId().getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void getWorkspaceNotFound() {
        client.get().uri("/workspace/1")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();

        Workspace workspace = new Workspace(1, user, "GET");
        workspace.setActive(new Date());
        workspace = workspaceRepository.save(workspace);
        client.get().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void putWorkspaceSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "PUT"));
        WorkspaceRequest request = new WorkspaceRequest(null);
        Workspace result = client.put().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Workspace.class)
                .returnResult()
                .getResponseBody();
        assertEquals(workspace.getId().getId(), result.getId().getId());
        assertEquals(workspace.getName(), result.getName());
        assertEquals(workspace, workspaceRepository.findByIdOrThrow(workspace.getId()));

        request.setName("NEW PUT");
        result = client.put().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(Workspace.class)
                .returnResult()
                .getResponseBody();
        assertEquals(workspace.getId().getId(), result.getId().getId());
        assertEquals(request.getName(), result.getName());
        assertNotEquals(workspace, workspaceRepository.findByIdOrThrow(workspace.getId()));
    }

    @Test
    void putWorkspaceBadRequest() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "PUT"));
        WorkspaceRequest request = new WorkspaceRequest("0".repeat(51));
        client.put().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void putWorkspaceUnathorized() {
        WorkspaceRequest request = new WorkspaceRequest("NEW PUT");
        client.put().uri("/workspace/1")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();

        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "PUT"));

        client.put().uri("/workspace/" + workspace.getId().getId())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void putWorkspaceNotFound() {
        WorkspaceRequest request = new WorkspaceRequest("NEW PUT");
        client.put().uri("/workspace/1")
                .cookie("session", session.getSession())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteWorkspaceSuccess() {
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        client.delete().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isOk();
        assertNotEquals(null, workspaceRepository.findById(workspace.getId()).get().getActive());
    }

    @Test
    void deleteWorkspaceBadRequest(@Autowired ProjectRepository projectRepository,
            @Autowired ProjectWorkspaceRepository projectWorkspaceRepository) {
        CounterSequence cs1 = counterSequenceRepository.save(new CounterSequence());
        CounterSequence cs2 = counterSequenceRepository.save(new CounterSequence());
        CounterSequence cs3 = counterSequenceRepository.save(new CounterSequence());
        Project project = projectRepository.save(new Project("DELETE", cs1, cs2, cs3));
        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));

        client.delete().uri("/workspace/" + workspace.getId().getId())
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isBadRequest();

        assertEquals(workspace.getActive(), workspaceRepository.findByIdOrThrow(workspace.getId()).getActive());
    }

    @Test
    void deleteWorkspaceUnauthorized() {
        client.delete().uri("/workspace/1")
                .exchange()
                .expectStatus().isUnauthorized();

        Workspace workspace = workspaceRepository.save(new Workspace(1, user, "DELETE"));
        client.delete().uri("/workspace/" + workspace.getId().getId())
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void deleteWorkspaceNotFound() {
        client.delete().uri("/workspace/1")
                .cookie("session", session.getSession())
                .exchange()
                .expectStatus().isNotFound();
    }
}
