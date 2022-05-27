package com.workflow.workflow.user.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import com.workflow.workflow.counter.CounterSequence;
import com.workflow.workflow.user.PasswordRecovery;
import com.workflow.workflow.user.PasswordRecoveryRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.utils.NotFoundRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final char[] CHARS = "0123456789qwertyuiopasdfghjklzxcvbnmQWERTYUIOPASDFGHJKLZXCVBNM".toCharArray();
    public static final String COOKIE_NAME = "session";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    private static String generateRandomString() {
        char[] b = new char[128];
        for (int i = 0; i < b.length; i++) {
            b[i] = CHARS[SECURE_RANDOM.nextInt(CHARS.length)];
        }
        return new String(b);
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private JavaMailSender javaMailSender;

    @Autowired
    private PasswordRecoveryRepository passwordRecoveryRepository;

    @Operation(summary = "Logged user", description = "This method returns currently logged user.")
    @ApiResponse(responseCode = "200", description = "Logged user.")
    @ApiResponse(responseCode = "404", description = "User is not logged.", content = @Content())
    @GetMapping("/me")
    public User me(@NotNull @Parameter(hidden = true) User loggedUser) {
        return loggedUser;
    }

    @Operation(summary = "Logging in", description = "This method logs the user in.")
    @ApiResponse(responseCode = "200", description = "Logged user.", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
    })
    @ApiResponse(responseCode = "403", description = "User is already logged.", content = @Content())
    @ApiResponse(responseCode = "404", description = "Username or password is incorrect.", content = @Content())
    @PostMapping("/login")
    public Future<User> login(@Parameter(hidden = true) User loggedUser, @RequestBody LoginRequest req,
            HttpServletRequest request, HttpServletResponse response) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        if (req.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing password");
        }
        if (req.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing username");
        }
        User u = req.getEmail().indexOf('@') != -1 ? userRepository.findByEmail(req.getEmail())
                : userRepository.findByUsername(req.getEmail());
        CompletableFuture<User> f = new CompletableFuture<>();
        EXECUTOR_SERVICE.schedule(() -> {
            if (u == null || !u.checkPassword(req.getPassword())) {
                f.completeExceptionally(
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "username or password incorrect"));
            } else {
                createSession(request, response, u, req.isRemember());
                f.complete(u);
            }
        }, 500 + RANDOM.nextInt(500), TimeUnit.MILLISECONDS);
        return f;
    }

    @Operation(summary = "Modify user account", description = "This method edits the account.")
    @ApiResponse(responseCode = "200", description = "User after changes.")
    @PutMapping("/edit")
    public User edit(@NotNull @Parameter(hidden = true) User loggedUser, @RequestBody EditAccountRequest req) {
        if (req.getPassword() != null) {
            loggedUser.setPassword(req.getPassword());
        }
        if (req.getAvatar() != null) {
            loggedUser.setAvatar(req.getAvatar());
        }
        if (req.getName() != null) {
            loggedUser.setName(req.getName());
        }
        if (req.getSurname() != null) {
            loggedUser.setSurname(req.getSurname());
        }
        userRepository.save(loggedUser);
        return loggedUser;
    }

    @Operation(summary = "Register account", description = "This method registers a new account. On success returns newly created user.")
    @ApiResponse(responseCode = "200", description = "Newly created user.")
    @ApiResponse(responseCode = "403", description = "User is already logged.", content = @Content())
    @ApiResponse(responseCode = "422", description = "Username or email is already taken.", content = @Content())
    @PostMapping("/register")
    public User register(@Parameter(hidden = true) User loggedUser, @RequestBody RegisterRequest req,
            HttpServletRequest request, HttpServletResponse response) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        if (req.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing email");
        }
        if (req.getName() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing name");
        }
        if (req.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing password");
        }
        if (req.getSurname() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing surname");
        }
        if (req.getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing username");
        }
        if (req.getUsername().indexOf('@') != -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "invalid character in username");
        }
        if (req.getEmail().indexOf('@') == -1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing at sign in email");
        }
        if (userRepository.findByUsername(req.getUsername()) != null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "this username is already taken");
        }
        if (userRepository.findByEmail(req.getEmail()) != null) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "this email is already taken");
        }
        User u = new User();
        u.setEmail(req.getEmail());
        u.setName(req.getName());
        u.setPassword(req.getPassword());
        u.setSurname(req.getSurname());
        u.setUsername(req.getUsername());
        u.setCounterSequence(new CounterSequence());
        u = userRepository.save(u);
        createSession(request, response, u, false);
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(req.getEmail());
        // TODO activation link
        msg.setSubject("Dziękujemy za rejestrację");
        msg.setText("Cześć, " + req.getName() + "!\nDziękujemy za zarejestrowanie się w naszym serwisie");
        javaMailSender.send(msg);
        return u;
    }

    // TODO make description (logout)
    @PostMapping("/logout")
    public void destroySession(HttpServletRequest req, HttpServletResponse resp) {
        Cookie cookie = new Cookie(COOKIE_NAME, null);
        cookie.setPath("/api");
        cookie.setMaxAge(0);
        resp.addCookie(cookie);
    }

    // TODO make description (recover passwd)
    @PostMapping("/recoverPassword")
    public void recoverPassword(@Parameter(hidden = true) User loggedUser, @RequestBody PasswordRecoveryRequest req) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        // ha tfu na timing atacki XD
        EXECUTOR_SERVICE.execute(() -> {
            User u = userRepository.findByEmail(req.getEmail());
            if (u == null) {
                return;
            }
            PasswordRecovery p = new PasswordRecovery();
            p.setUser(u);
            p.setActive(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
            p.setToken(generateRandomString());
            while (true) {
                try {
                    p = passwordRecoveryRepository.save(p);
                    break;
                } catch (DataIntegrityViolationException ex) {
                    p.setToken(generateRandomString());
                }
            }
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(req.getEmail());
            msg.setSubject("Zapomniałeś hasła?");
            msg.setText("Cześć, " + u.getName()
                    + "!\nJeśli zapomniałeś hasła to wejdź w link: https://workflow.adiantek.ovh/pl-PL/auth/set-new-password?token="
                    + p.getToken() + "\nLink wygaśnie po 30 minutach");
            javaMailSender.send(msg);
        });
    }

    @PostMapping("/resetPassword")
    public void resetPassword(@Parameter(hidden = true) User loggedUser, @RequestBody ResetPasswordRequest req) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        PasswordRecovery p = passwordRecoveryRepository.findByToken(req.getToken());
        if (p == null) {
            throw NotFoundRepository.getException();
        }
        if (req.getPassword() == null) {
            return;
        }
        User u = p.getUser();
        passwordRecoveryRepository.delete(p);
        u.setPassword(req.getPassword());
        userRepository.save(u);
    }

    private void createSession(HttpServletRequest req, HttpServletResponse resp, User user, boolean remembered) {
        UserSession us = new UserSession();
        us.setSession(generateRandomString());
        us.setIp(req.getHeader("X-Forwarded-For"));
        if (us.getIp() == null) {
            us.setIp(req.getRemoteAddr());
        }
        us.setLastUsed(new Date());
        us.setRemembered(remembered);
        us.setUserAgent(req.getHeader("User-Agent"));
        us.setUser(user);
        while (true) {
            try {
                us = userSessionRepository.save(us);
                break;
            } catch (DataIntegrityViolationException ex) {
                us.setSession(generateRandomString());
            }
        }
        Cookie c = new Cookie(COOKIE_NAME, us.getSession());
        c.setPath("/api");
        if (req.getHeader("X-Forwarded-For") != null) {
            c.setSecure(true);
        } else {
            c.setHttpOnly(true);
        }
        resp.addCookie(c);
    }
}
