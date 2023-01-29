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

 package dev.vernite.vernite.task.time;

 import static org.junit.jupiter.api.Assertions.assertEquals;
 import static org.junit.jupiter.api.Assertions.assertNotNull;
 import static org.junit.jupiter.api.Assertions.assertNull;
 
 import java.time.Instant;
 import java.time.temporal.ChronoUnit;
 import java.util.Date;
 
 import org.junit.jupiter.api.BeforeAll;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.Test;
 import org.junit.jupiter.api.TestInstance;
 import org.junit.jupiter.api.TestInstance.Lifecycle;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
 import org.springframework.boot.test.context.SpringBootTest;
 import org.springframework.dao.DataIntegrityViolationException;
 import org.springframework.http.HttpStatus;
 import org.springframework.test.context.TestPropertySource;
 import org.springframework.test.web.reactive.server.WebTestClient;
 
 import dev.vernite.vernite.project.Project;
 import dev.vernite.vernite.project.ProjectRepository;
 import dev.vernite.vernite.projectworkspace.ProjectWorkspace;
 import dev.vernite.vernite.projectworkspace.ProjectWorkspaceRepository;
 import dev.vernite.vernite.status.Status;
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
 class TimeTrackControllerTests {
     @Autowired
     private WebTestClient client;
     @Autowired
     private UserRepository userRepository;
     @Autowired
     private UserSessionRepository sessionRepository;
     @Autowired
     private ProjectRepository projectRepository;
     @Autowired
     private WorkspaceRepository workspaceRepository;
     @Autowired
     private ProjectWorkspaceRepository projectWorkspaceRepository;
     @Autowired
     private TaskRepository taskRepository;
     @Autowired
     private TimeTrackRepository timeTrackRepository;
 
     private User user;
     private UserSession session;
     private Project project;
     private Project forbiddenProject;
 
     void taskEquals(Task expected, Task actual) {
         assertEquals(expected.getName(), actual.getName());
         assertEquals(expected.getDeadline(), actual.getDeadline());
         assertEquals(expected.getDescription(), actual.getDescription());
         assertEquals(expected.getEstimatedDate(), actual.getEstimatedDate());
         assertEquals(expected.getIssue(), actual.getIssue());
         assertEquals(expected.getPriority(), actual.getPriority());
         assertEquals(expected.getPull(), actual.getPull());
         assertEquals(expected.getStoryPoints(), actual.getStoryPoints());
     }
 
     @BeforeAll
     void init() {
         this.user = userRepository.findByUsername("Username");
         if (this.user == null) {
             this.user = userRepository.save(new User("Name", "Surname", "Username", "Email@test.pl", "1"));
         }
         session = new UserSession();
         session.setIp("127.0.0.1");
         session.setSession("session_token_tasks_tests");
         session.setLastUsed(new Date());
         session.setRemembered(true);
         session.setUserAgent("userAgent");
         session.setUser(user);
         try {
             session = sessionRepository.save(session);
         } catch (DataIntegrityViolationException e) {
             session = sessionRepository.findBySession("session_token_tasks_tests").orElseThrow();
         }
         project = new Project("Tasks project", "");
         project.getStatuses().add(new Status("To Do", 0, 0, false, true, project));
         project.getStatuses().add(new Status("In Progress", 0, 1, false, false, project));
         project.getStatuses().add(new Status("Done", 0, 2, true, false, project));
         project = projectRepository.save(project);
         forbiddenProject = new Project("Tasks project forbidden", "");
         forbiddenProject.getStatuses().add(new Status("To Do", 0, 0, false, true, forbiddenProject));
         forbiddenProject.getStatuses().add(new Status("In Progress", 0, 1, false, false, forbiddenProject));
         forbiddenProject.getStatuses().add(new Status("Done", 0, 2, true, false, forbiddenProject));
         forbiddenProject = projectRepository.save(forbiddenProject);
         Workspace workspace = workspaceRepository.save(new Workspace(1, "tasks test workspace", user));
         projectWorkspaceRepository.save(new ProjectWorkspace(project, workspace, 1L));
     }
 
     @BeforeEach
     void reset() {
         taskRepository.deleteAll();
     }
 
     @Test
     void createTrackingSuccess() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
 
         Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
 
         TimeTrack track = client.post()
                 .uri("/project/{pId}/task/{id}/track", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession())
                 .bodyValue(new CreateTimeTrack(date, new Date()))
                 .exchange().expectStatus().isOk().expectBody(TimeTrack.class).returnResult().getResponseBody();
         assertNotNull(track);
         assertEquals(true, track.isEdited());
         assertEquals(date, track.getStartDate());
     }
 
     @Test
     void createTrackingBadRequest() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
 
         client.post()
                 .uri("/project/{pId}/task/{id}/track", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new CreateTimeTrack())
                 .exchange().expectStatus().isBadRequest();
 
         client.post()
                 .uri("/project/{pId}/task/{id}/track", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession())
                 .bodyValue(new CreateTimeTrack(new Date(), null))
                 .exchange().expectStatus().isBadRequest();
 
         Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
 
         client.post()
                 .uri("/project/{pId}/task/{id}/track", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession())
                 .bodyValue(new CreateTimeTrack(new Date(), date))
                 .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
     }
 
     @Test
     void createTrackingUnauthorized() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track", project.getId(), task.getNumber())
                 .bodyValue(new CreateTimeTrack()).exchange().expectStatus().isUnauthorized();
     }
 
     @Test
     void createTrackingNotFound() {
         client.post().uri("/project/{pId}/task/{tId}/track", project.getId(), Long.MAX_VALUE)
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new CreateTimeTrack(new Date(), new Date()))
                 .exchange().expectStatus().isNotFound();
         Task task = taskRepository
                 .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new CreateTimeTrack(new Date(), new Date()))
                 .exchange().expectStatus().isNotFound();
 
         task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new CreateTimeTrack(new Date(), new Date()))
                 .exchange().expectStatus().isNotFound();
     }
 
     @Test
     void startTrackingSuccess() throws InterruptedException {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
 
         TimeTrack track = client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                 .expectBody(TimeTrack.class).returnResult().getResponseBody();
         assertNotNull(track);
         assertNull(track.getEndDate());
         assertEquals(false, track.isEdited());
         assertNotNull(track.getStartDate());
     }
 
     @Test
     void startTrackingUnauthorized() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber()).exchange()
                 .expectStatus().isUnauthorized();
     }
 
     @Test
     void startTrackingNotFound() {
         client.post().uri("/project/{pId}/task/1/track/start", project.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
         Task task = taskRepository
                 .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track/start", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                 .expectStatus().isNotFound();
     }
 
     @Test
     void startTrackingConflict() throws InterruptedException {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
 
         client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                 .expectBody(TimeTrack.class);
 
         client.post().uri("/project/{pId}/task/{id}/track/start", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus()
                 .isEqualTo(HttpStatus.CONFLICT);
     }
 
     @Test
     void stopTrackingSuccess() throws InterruptedException {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         timeTrackRepository.save(new TimeTrack(user, task));
 
         TimeTrack track = client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                 .expectBody(TimeTrack.class).returnResult().getResponseBody();
         assertNotNull(track);
         assertNotNull(track.getEndDate());
         assertEquals(false, track.isEdited());
         assertNotNull(track.getStartDate());
     }
 
     @Test
     void stopTrackingUnauthorized() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber()).exchange()
                 .expectStatus().isUnauthorized();
     }
 
     @Test
     void stopTrackingNotFound() {
         client.post().uri("/project/{pId}/task/1/track/stop", project.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
         Task task = taskRepository
                 .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
         client.post().uri("/project/{pId}/task/{id}/track/stop", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                 .expectStatus().isNotFound();
     }
 
     @Test
     void stopTrackingConflict() throws InterruptedException {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
 
         client.post().uri("/project/{pId}/task/{id}/track/stop", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus()
                 .isEqualTo(HttpStatus.CONFLICT);
     }
 
     @Test
     void editTrackingSuccess() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         TimeTrack timeTrack = new TimeTrack(user, task);
         timeTrack.setEndDate(new Date());
         timeTrack = timeTrackRepository.save(timeTrack);
 
         Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
 
         TimeTrack track = client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack(date, null))
                 .exchange().expectStatus().isOk().expectBody(TimeTrack.class).returnResult().getResponseBody();
         assertNotNull(track);
         assertEquals(true, track.isEdited());
         assertEquals(date, track.getStartDate());
 
         date = new Date();
 
         track = client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack(null, date))
                 .exchange().expectStatus().isOk().expectBody(TimeTrack.class).returnResult().getResponseBody();
 
         assertNotNull(track);
         assertEquals(true, track.isEdited());
         assertEquals(date, track.getEndDate());
     }
 
     @Test
     void editTrackingBadRequest() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         TimeTrack timeTrack = new TimeTrack(user, task);
         timeTrack = timeTrackRepository.save(timeTrack);
 
         client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack())
                 .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
 
         timeTrack.setEndDate(new Date());
         timeTrackRepository.save(timeTrack);
         Date date = Date.from(Instant.now().minus(1, ChronoUnit.DAYS));
 
         client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack(null, date))
                 .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
 
         date = Date.from(Instant.now().plus(1, ChronoUnit.DAYS));
 
         client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack(null, date))
                 .exchange().expectStatus().isEqualTo(HttpStatus.CONFLICT);
     }
 
     @Test
     void editTrackingUnauthorized() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.put().uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), 1)
                 .bodyValue(new UpdateTimeTrack()).exchange().expectStatus().isUnauthorized();
     }
 
     @Test
     void editTrackingNotFound() {
         client.put().uri("/project/{pId}/task/1/track/1", project.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack())
                 .exchange().expectStatus().isNotFound();
         Task task = taskRepository
                 .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
         client.put().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack())
                 .exchange().expectStatus().isNotFound();
 
         task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.put().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack())
                 .exchange().expectStatus().isNotFound();
 
         task = taskRepository
                 .save(new Task(3, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         Task task2 = taskRepository.save(new Task(2, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task2));
 
         client.put()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).bodyValue(new UpdateTimeTrack(null, null))
                 .exchange().expectStatus().isNotFound();
     }
 
     @Test
     void deleteTrackingSuccess() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task));
         client.delete()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk();
     }
 
     @Test
     void deleteTrackingUnauthorized() {
         Task task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.delete().uri("/project/{pId}/task/{id}/track/1", project.getId(), task.getNumber())
                 .exchange().expectStatus().isUnauthorized();
     }
 
     @Test
     void deleteTrackingNotFound() {
         client.delete().uri("/project/{pId}/task/1/track/1", project.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isNotFound();
         Task task = taskRepository
                 .save(new Task(1, "NAME", "DESC", forbiddenProject.getStatuses().get(0), user, 0, "low"));
         client.delete().uri("/project/{pId}/task/{id}/track/1", forbiddenProject.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                 .expectStatus().isNotFound();
 
         task = taskRepository.save(new Task(1, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         client.delete().uri("/project/{pId}/task/{id}/track/1", project.getId(), task.getNumber())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                 .expectStatus().isNotFound();
 
         Task task2 = taskRepository.save(new Task(2, "NAME", "DESC", project.getStatuses().get(0), user, 0, "low"));
         TimeTrack timeTrack = timeTrackRepository.save(new TimeTrack(user, task2));
         client.delete()
                 .uri("/project/{pId}/task/{id}/track/{trackId}", project.getId(), task.getNumber(), timeTrack.getId())
                 .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange()
                 .expectStatus().isNotFound();
     }
 }
 