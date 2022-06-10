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
        userRepository.deleteAll();
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
