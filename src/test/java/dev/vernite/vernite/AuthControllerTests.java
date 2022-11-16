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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;
import dev.vernite.vernite.user.auth.ChangePasswordRequest;
import dev.vernite.vernite.user.auth.LoginRequest;
import dev.vernite.vernite.user.auth.PasswordRecoveryRequest;
import dev.vernite.vernite.user.auth.RegisterRequest;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class AuthControllerTests {
    
    @Autowired
    private WebTestClient client;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private UserRepository userRepository;
    
    @BeforeEach
    void reset() {
        userSessionRepository.deleteAll();
        userRepository.deleteAllByEmailNot("contact@vernite.dev");
    }

    @Test
    void loginByUsername() {
        User u = new User("name", "surname", "usernameX", "contact+7@vernite.dev", "password", "English", "YYYY-MM-DD");
        User registeredUser = userRepository.save(u);

        LoginRequest req = new LoginRequest();
        req.setEmail(u.getUsername());
        req.setPassword("password");
        req.setRemember(true);

        client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(registeredUser.getId(), user.getId());
                    assertEquals(registeredUser.getUsername(), user.getUsername());
                    assertEquals(registeredUser.getName(), user.getName());
                    assertEquals(registeredUser.getSurname(), user.getSurname());
                    assertEquals(registeredUser.getEmail(), user.getEmail());
                });
    }

    @Test
    void loginByEmailAndChangePassword() {
        User u = new User("name", "surname", "username2", "contact+6@vernite.dev", "password");
        User registeredUser = userRepository.save(u);

        LoginRequest req = new LoginRequest();
        req.setEmail(u.getEmail());
        req.setPassword("password");
        req.setRemember(true);

        ResponseCookie cookie = client.post()
                .uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class)
                .value(user -> {
                    assertEquals(registeredUser.getId(), user.getId());
                    assertEquals(registeredUser.getUsername(), user.getUsername());
                    assertEquals(registeredUser.getName(), user.getName());
                    assertEquals(registeredUser.getSurname(), user.getSurname());
                    assertEquals(registeredUser.getEmail(), user.getEmail());
                })
                .returnResult().getResponseCookies().getFirst(AuthController.COOKIE_NAME);
        assertNotNull(cookie);
        client.post()
                .uri("/auth/login")
                .cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isForbidden();
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("badPassword");
        changePasswordRequest.setNewPassword("newPassword");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isNotFound();
        
        changePasswordRequest.setOldPassword("password");
        changePasswordRequest.setNewPassword("");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isBadRequest();
        
        changePasswordRequest.setNewPassword("newPassword");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/auth/logout").cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resetPassword() {
        User u = new User("name", "surname", "username2", "contact+4@vernite.dev", "password", "English", "YYYY-MM-DD");
        userRepository.save(u);

        PasswordRecoveryRequest req = new PasswordRecoveryRequest();
        req.setEmail("contact@vernite.dev");
        client.post().uri("/auth/password/recover")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void register() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("test");
        req.setPassword("test");
        req.setEmail("contact+5@vernite.dev");
        req.setName("test name");
        req.setSurname("test surname");
        req.setLanguage("English");
        req.setDateFormat("YYYY-MM-DD");

        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class).value(u -> {
                    assertEquals(u.getUsername(), req.getUsername());
                    assertEquals(u.getEmail(), req.getEmail());
                    assertEquals(u.getName(), req.getName());
                    assertEquals(u.getSurname(), req.getSurname());
                    assertEquals(u.getLanguage(), req.getLanguage());
                    assertEquals(u.getDateFormat(), req.getDateFormat());
                });
    }

    @Test
    void getUserEventsSuccess() {
        User u = new User("name", "surname", "usernameX", "contact+3@vernite.dev", "password", "English", "YYYY-MM-DD");
        User registeredUser = userRepository.save(u);

        UserSession session = new UserSession();
        session.setIp("127.0.0.1");
        session.setSession("session_token_events_tests");
        session.setLastUsed(new Date());
        session.setRemembered(true);
        session.setUserAgent("userAgent");
        session.setUser(registeredUser);
        try {
            session = userSessionRepository.save(session);
        } catch (DataIntegrityViolationException e) {
            session = userSessionRepository.findBySession("session_token_projects_tests").orElseThrow();
        }

        client.get().uri("/auth/me/events?from=1&to=1000")
                .cookie(AuthController.COOKIE_NAME, session.getSession()).exchange().expectStatus().isOk()
                .expectBodyList(Event.class).hasSize(0);
    }
}
