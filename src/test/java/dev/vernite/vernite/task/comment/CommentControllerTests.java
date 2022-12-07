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

package dev.vernite.vernite.task.comment;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

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
import dev.vernite.vernite.status.Status;
import dev.vernite.vernite.status.StatusRepository;
import dev.vernite.vernite.task.Task;
import dev.vernite.vernite.task.TaskRepository;
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
class CommentControllerTests {
    @Autowired
    private WebTestClient client;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserSessionRepository sessionRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ProjectRepository projectRepository;
    @Autowired
    private StatusRepository statusRepository;
    @Autowired
    private TaskRepository taskRepository;
    @Autowired
    private ProjectWorkspaceRepository projectWorkspaceRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    private User user;
    private UserSession session;
    private Task task;
    private Task task2;
    private Project project;

    @BeforeAll
    void init() {
        this.user = userRepository.findByUsername("Username");
        if (this.user == null) {
            this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
        }
        session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_comment_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(user);
        try {
            session = sessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = sessionRepository.findBySession("session_token_comment_tests").orElseThrow();
        }

        project = projectRepository.save(new Project("Project", "Description"));
        var workspace = workspaceRepository.save(new Workspace(1, "name", user));
        projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
        var status = statusRepository.save(new Status("name", 0, 0, false, false, project));
        task = taskRepository.save(new Task(1, "name", "desc", status, user, 0, "low"));
        task2 = taskRepository.save(new Task(2, "name", "desc", status, user, 0, "low"));
    }

    @BeforeEach
    void clean() {
        commentRepository.deleteAll();
    }

    @Test
    void getAllSuccess() {
        client.get().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Comment.class).hasSize(0);

        List<Comment> comments = List.of(new Comment(task, "Content 1", user), new Comment(task, "Content 2", user));
        comments.get(0).setCreatedAt(new Date(0));

        commentRepository.saveAll(comments);

        client.get().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBodyList(Comment.class).hasSize(2).contains(comments.get(0))
                .contains(comments.get(1));
    }

    @Test
    void getAllUnauthorized() {
        client.get().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .exchange().expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, "invalid_session")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void createSuccess() {
        client.post().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateComment("Content")).exchange().expectStatus().isOk()
                .expectBody(Comment.class).value(comment -> comment.getContent().equals("Content"));
    }

    @Test
    void createBadRequest() {
        client.post().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateComment("")).exchange().expectStatus().isBadRequest();

        client.post().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new CreateComment(null)).exchange().expectStatus().isBadRequest();
    }

    @Test
    void createUnauthorized() {
        client.post().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .exchange().expectStatus().isUnauthorized();

        client.post().uri("/project/{projectId}/task/{taskId}/comment", project.getId(), task.getNumber())
                .cookie(AuthController.COOKIE_NAME, "invalid_session")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getSuccess() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.get().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk().expectBody(Comment.class).value(responseComment -> {
                    assertEquals(comment.getContent(), responseComment.getContent());
                    assertEquals(comment.getCreatedAt(), responseComment.getCreatedAt());
                });
    }

    @Test
    void getUnauthorized() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.get().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).exchange().expectStatus().isUnauthorized();

        client.get().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, "invalid_session")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void getNotFound() {
        client.get().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                1L).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();

        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.get().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task2.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

    @Test
    void updateSuccess() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateComment("New content")).exchange().expectStatus().isOk()
                .expectBody(Comment.class).value(updatedComment -> updatedComment.getContent().equals("New content"));
    }

    @Test
    void updateBadRequest() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateComment("")).exchange().expectStatus().isBadRequest();

        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateComment("a".repeat(1001))).exchange().expectStatus().isBadRequest();
    }

    @Test
    void updateUnauthorized() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).exchange().expectStatus().isUnauthorized();

        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, "invalid_session")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void updateNotFound() {
        client.put().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .bodyValue(new UpdateComment("New content")).exchange().expectStatus().isNotFound();
    }

    @Test
    void deleteSuccess() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.delete().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isOk();

        assertFalse(commentRepository.findById(comment.getId()).isPresent());
    }

    @Test
    void deleteUnauthorized() {
        Comment comment = commentRepository.save(new Comment(task, "Content", user));

        client.delete().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).exchange().expectStatus().isUnauthorized();

        client.delete().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                comment.getId()).cookie(AuthController.COOKIE_NAME, "invalid_session")
                .exchange().expectStatus().isUnauthorized();
    }

    @Test
    void deleteNotFound() {
        client.delete().uri("/project/{projectId}/task/{taskId}/comment/{commentId}", project.getId(), task.getNumber(),
                1).cookie(AuthController.COOKIE_NAME, session.getSession())
                .exchange().expectStatus().isNotFound();
    }

}
