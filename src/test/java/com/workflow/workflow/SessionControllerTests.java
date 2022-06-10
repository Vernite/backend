package com.workflow.workflow;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

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
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.user.auth.AuthController;
import com.workflow.workflow.user.auth.ChangePasswordRequest;
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
        userRepository.deleteAll();
    }

    
    @Test
    void loginAndRevokeSession() {
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
        List<UserSession> list = client.get()
                .uri("/session")
                .cookie(AuthController.COOKIE_NAME, cookie)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserSession.class)
                .returnResult().getResponseBody();
        int revoked = 0;
        for (UserSession us : list) {
            if (!us.isCurrent()) {
                revoked++;
                client.delete()
                        .uri("/session/" + us.getId())
                        .cookie(AuthController.COOKIE_NAME, cookie)
                        .exchange()
                        .expectStatus().isOk();
            }
        }
        List<UserSession> list2 = client.get()
                .uri("/session")
                .cookie(AuthController.COOKIE_NAME, cookie)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(UserSession.class)
                .returnResult().getResponseBody();
        assertEquals(revoked + list2.size(), list.size());
    }
}
