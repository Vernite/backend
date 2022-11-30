package dev.vernite.vernite.release;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Date;
import java.util.List;

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
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.vernite.vernite.project.Project;
import dev.vernite.vernite.project.ProjectRepository;
import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;
import dev.vernite.vernite.workspace.Workspace;
import dev.vernite.vernite.workspace.WorkspaceRepository;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class ReleaseControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository userSessionRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private ReleaseRepository releaseRepository;

    private User user;
    private UserSession session;
    private Workspace workspace;
    private Project project;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_release_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_release_tests").orElseThrow();
        }
        workspace = workspaceRepository.save(new Workspace(1, "Project Tests", user));
        project = projectRepository.save(new Project("Sprint Tests"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
    }

    @BeforeEach
    void clean() {
        releaseRepository.deleteAll();
    }

    void releaseEquals(Release expected, Release actual) {
        assertEquals(expected.getName(), actual.getName());
        assertEquals(expected.getDescription(), actual.getDescription());
        assertEquals(expected.getDeadline(), actual.getDeadline());
        assertEquals(expected.getReleased(), actual.getReleased());
        assertEquals(expected.getProject().getId(), actual.getProject().getId());
    }

    @Test
    void getAllSuccess() {
        // Test empty return list
        client.get().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Release.class).hasSize(0);
        // Test non-empty return list
        List<Release> releases = List.of(
                new Release("Name 1", project),
                new Release("Name 3", project),
                new Release("Name 2", project));
        releaseRepository.saveAll(releases);
        List<Release> result = client.get().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Release.class).hasSize(3).returnResult().getResponseBody();
        assertNotNull(result);
        releaseEquals(result.get(0), releases.get(0));
        releaseEquals(result.get(1), releases.get(2));
        releaseEquals(result.get(2), releases.get(1));
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{projectId}/release", project.getId()).exchange().expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/release", project.getId()).cookie(AuthController.COOKIE_NAME, "invalid")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getAllNotFound() {
        client.get().uri("/project/{projectId}/release", 0).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.get().uri("/project/{projectId}/release", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void createSuccess() {
        Release release = client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name", null, null)).exchange()
                .expectStatus().isOk().expectBody(Release.class).returnResult().getResponseBody();
        assertNotNull(release);

        Release result = releaseRepository.findById(release.getId()).orElseThrow();
        releaseEquals(release, result);
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest(null, null, null))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest("", null, null))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest(" ", null, null))
                .exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest("a".repeat(51), null, null))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{projectId}/release", project.getId())
                .bodyValue(new ReleaseRequest("Name", null, null)).exchange()
                .expectStatus().isUnauthorized();

        client.post().uri("/project/{projectId}/release", project.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid")
                .bodyValue(new ReleaseRequest("Name", null, null)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void createNotFound() {
        client.post().uri("/project/{projectId}/release", 0)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name", null, null)).exchange()
                .expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        client.post().uri("/project/{projectId}/release", project2.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name", null, null)).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getSuccess() {
        Release release = releaseRepository.save(new Release("Name", project));
        Release result = client.get().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBody(Release.class).returnResult().getResponseBody();
        assertNotNull(result);
        releaseEquals(release, result);
    }

    @Test
    void getUnauthorized() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.get().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId()).exchange()
                .expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.get().uri("/project/{projectId}/release/{releaseId}", project.getId(), 0)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        client.get().uri("/project/{projectId}/release/{releaseId}", 0, release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));
        client.get().uri("/project/{projectId}/release/{releaseId}", project2.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Release release = releaseRepository.save(new Release("Name", project));
        Release result = client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name 2", "null", null)).exchange().expectStatus().isOk()
                .expectBody(Release.class).returnResult().getResponseBody();
        release.setName("Name 2");
        release.setDescription("null");
        assertNotNull(result);
        releaseEquals(release, result);
        assertEquals("Name 2", result.getName());
    }

    @Test
    void updateBadRequest() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest(null, null, null))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest("", null, null))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest(" ", null, null))
                .exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new ReleaseRequest("a".repeat(51), null, null))
                .exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .bodyValue(new ReleaseRequest("Name 2", null, null)).exchange()
                .expectStatus().isUnauthorized();

        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid")
                .bodyValue(new ReleaseRequest("Name 2", null, null)).exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.put().uri("/project/{projectId}/release/{releaseId}", project.getId(), 0)
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name 2", null, null)).exchange()
                .expectStatus().isNotFound();

        client.put().uri("/project/{projectId}/release/{releaseId}", 0, release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name 2", null, null)).exchange()
                .expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));
        client.put().uri("/project/{projectId}/release/{releaseId}", project2.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new ReleaseRequest("Name 2", null, null)).exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.delete().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
        assertNotNull(releaseRepository.findById(release.getId()).get().getActive());
    }

    @Test
    void deleteUnauthorized() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.delete().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId()).exchange()
                .expectStatus().isUnauthorized();

        client.delete().uri("/project/{projectId}/release/{releaseId}", project.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, "invalid").exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteNotFound() {
        Release release = releaseRepository.save(new Release("Name", project));
        client.delete().uri("/project/{projectId}/release/{releaseId}", project.getId(), 0)
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        client.delete().uri("/project/{projectId}/release/{releaseId}", 0, release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();

        Project project2 = projectRepository.save(new Project("Sprint Tests 2"));
        projectWorkspaceRepository.save(new ProjectWorkspace(project2, workspace, 1L));
        client.delete().uri("/project/{projectId}/release/{releaseId}", project2.getId(), release.getId())
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
    }
}
