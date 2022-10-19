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
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.user.auth.LoginRequest;

@SpringBootTest
@AutoConfigureMockMvc
@TestInstance(Lifecycle.PER_CLASS)
@TestPropertySource({ "classpath:application.properties", "classpath:application-test.properties" })
public class SessionControllerTests {
    
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
    void loginAndRevokeSession() {
        User u = new User("name", "surname", "username2", "email@127.0.0.1", "password");
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
        List<UserSession> list = client.get()
                .uri("/session")
                .cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserSession.class)
                .returnResult().getResponseBody();
        assertNotNull(list);
        int revoked = 0;
        for (UserSession us : list) {
            if (!us.isCurrent()) {
                revoked++;
                client.delete()
                        .uri("/session/" + us.getId())
                        .cookie(AuthController.COOKIE_NAME, cookie.getValue())
                        .exchange()
                        .expectStatus().isOk();
            }
        }
        List<UserSession> list2 = client.get()
                .uri("/session")
                .cookie(AuthController.COOKIE_NAME, cookie.getValue())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserSession.class)
                .returnResult().getResponseBody();
        assertNotNull(list2);
        assertEquals(revoked + list2.size(), list.size());
    }
}
