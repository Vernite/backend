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

package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.user.auth.ChangePasswordRequest;
import com.workflow.workflow.user.auth.LoginRequest;
import com.workflow.workflow.user.auth.PasswordRecoveryRequest;
import com.workflow.workflow.user.auth.RegisterRequest;

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
        userRepository.deleteAllByEmailNot("wflow1337@gmail.com");
    }

    @Test
    void loginByUsername() {
        User u = new User("name", "surname", "usernameX", "email@127.0.0.1", "password");
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
        User u = new User("name", "surname", "username2", "email@127.0.0.1", "password");
        User registeredUser = userRepository.save(u);

        LoginRequest req = new LoginRequest();
        req.setEmail(u.getEmail());
        req.setPassword("password");
        req.setRemember(true);

        String cookie = client.post()
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
                .returnResult().getResponseCookies().getFirst(AuthController.COOKIE_NAME).getValue();
        client.post()
                .uri("/auth/login")
                .cookie(AuthController.COOKIE_NAME, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(req)
                .exchange()
                .expectStatus().isForbidden();
        ChangePasswordRequest changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setOldPassword("badPassword");
        changePasswordRequest.setNewPassword("newPassword");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isNotFound();
        
        changePasswordRequest.setOldPassword("password");
        changePasswordRequest.setNewPassword("");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isBadRequest();
        
        changePasswordRequest.setNewPassword("newPassword");
        client.post().uri("/auth/password/change").cookie(AuthController.COOKIE_NAME, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isOk();

        client.post().uri("/auth/logout").cookie(AuthController.COOKIE_NAME, cookie)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(changePasswordRequest)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void resetPassword() {
        User u = new User("name", "surname", "username2", "email@127.0.0.1", "password");
        userRepository.save(u);

        PasswordRecoveryRequest req = new PasswordRecoveryRequest();
        req.setEmail("email@127.0.0.1");
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
        req.setEmail("test@127.0.0.1");
        req.setName("test name");
        req.setSurname("test surname");

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
                });
    }
}
