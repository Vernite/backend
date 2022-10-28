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

package com.workflow.workflow.user.auth;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
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
import com.workflow.workflow.task.time.TimeTrack;
import com.workflow.workflow.task.time.TimeTrackRepository;
import com.workflow.workflow.user.DeleteAccountRequest;
import com.workflow.workflow.user.DeleteAccountRequestRepository;
import com.workflow.workflow.user.PasswordRecovery;
import com.workflow.workflow.user.PasswordRecoveryRepository;
import com.workflow.workflow.user.User;
import com.workflow.workflow.user.UserRepository;
import com.workflow.workflow.user.UserSession;
import com.workflow.workflow.user.UserSessionRepository;
import com.workflow.workflow.utils.ObjectNotFoundException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
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

    private static String generateRandomSecureString() {
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
    private DeleteAccountRequestRepository deleteAccountRepository;

    @Autowired
    private PasswordRecoveryRepository passwordRecoveryRepository;

    @Autowired
    private TimeTrackRepository timeTrackRepository;

    @Operation(summary = "Logged user", description = "This method returns currently logged user.")
    @ApiResponse(responseCode = "200", description = "Logged user.")
    @ApiResponse(responseCode = "401", description = "User is not logged.", content = @Content())
    @GetMapping("/me")
    public User me(@NotNull @Parameter(hidden = true) User loggedUser) {
        return loggedUser;
    }

    @Operation(summary = "Get time tracks", description = "This method gets time tracks for logged in user.")
    @ApiResponse(responseCode = "200", description = "Time tracks.")
    @ApiResponse(responseCode = "401", description = "User is not logged.", content = @Content())
    @GetMapping("/me/track")
    public List<TimeTrack> getTimeTracks(@NotNull @Parameter(hidden = true) User loggedUser) {
        return timeTrackRepository.findByUser(loggedUser);
    }

    @Operation(summary = "Delete account", description = "This method deletes currently logged user by sending an e-mail with a confirmation link.")
    @ApiResponse(responseCode = "200")
    @DeleteMapping("/delete")
    public void delete(@NotNull @Parameter(hidden = true) User loggedUser) {
        DeleteAccountRequest d = new DeleteAccountRequest();
        d.setUser(loggedUser);
        d.setActive(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
        d.setToken(generateRandomSecureString());
        while (true) {
            try {
                d = deleteAccountRepository.save(d);
                break;
            } catch (DataIntegrityViolationException ex) {
                d.setToken(generateRandomSecureString());
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(loggedUser.getEmail());
        message.setSubject("Potwierdzenie usunięcia Twojego konta");
        message.setText("Aby potwierdzić usuwanie Twojego konta, kliknij w poniższy link:\n" +
            "https://workflow.adiantek.ovh/pl-PL/auth/delete-account?token=" + d.getToken() + "\n" +
            "Link wygaśnie po 30 minutach");
        javaMailSender.send(message);
    }

    @Operation(summary = "Delete account", description = "This method deletes currently logged user after clicking on the confirmation link.")
    @ApiResponse(responseCode = "200", description = "Account deleted.")
    @ApiResponse(responseCode = "403", description = "Token is not compatible with the currently logged in user.", content = @Content())
    @ApiResponse(responseCode = "404", description = "Token is invalid.", content = @Content())
    @DeleteMapping("/delete/confirm")
    public void deleteConfirm(@NotNull @Parameter(hidden = true) User loggedUser, @RequestBody DeleteRequest request) {
        DeleteAccountRequest d = deleteAccountRepository.findByToken(request.getToken());
        if (d == null) {
            throw new ObjectNotFoundException();
        }
        if (d.getUser().getId() != loggedUser.getId()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid user");
        }
        deleteAccountRepository.delete(d);
        loggedUser.setDeleted(new Date());
        userRepository.save(loggedUser);
    }

    @Operation(summary = "Recover deleted account", description = "This method recovers a deleted account if it was deleted in less than 1 week.")
    @ApiResponse(responseCode = "200", description = "Recovered user.", content = {
            @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
    })
    @ApiResponse(responseCode = "401", description = "User is not logged.", content = @Content())
    @PostMapping("/delete/recover")
    public User recoverDeleted(@Parameter(hidden = true) User loggedUser) {
        if (loggedUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "user not logged");
        }
        if (loggedUser.getDeleted() == null) {
            return loggedUser;
        }
        loggedUser.setDeleted(null);
        this.userRepository.save(loggedUser);
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
        if (req.getAvatar() != null) {
            loggedUser.setAvatar(req.getAvatar());
        }
        if (req.getName() != null) {
            loggedUser.setName(req.getName());
        }
        if (req.getSurname() != null) {
            loggedUser.setSurname(req.getSurname());
        }
        if (req.getLanguage() != null) {
            loggedUser.setLanguage(req.getLanguage());
        }
        if (req.getDateFormat() != null) {
            loggedUser.setDateFormat(req.getDateFormat());
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
        u.setLanguage(req.getLanguage());
        u.setDateFormat(req.getDateFormat());
        u.setCounterSequence(new CounterSequence());
        u = userRepository.save(u);
        createSession(request, response, u, false);
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(req.getEmail());
        msg.setFrom("wflow1337@gmail.com");
        // TODO activation link
        msg.setSubject("Dziękujemy za rejestrację");
        msg.setText("Cześć, " + req.getName() + "!\nDziękujemy za zarejestrowanie się w naszym serwisie");
        javaMailSender.send(msg);
        return u;
    }

    @Operation(summary = "Log out", description = "This method log outs the user.")
    @ApiResponse(responseCode = "200", description = "User logged out")
    @PostMapping("/logout")
    public void destroySession(HttpServletRequest req, HttpServletResponse resp, @Parameter(hidden = true) @CookieValue(AuthController.COOKIE_NAME) String session) {
        if (session != null) {
            this.userSessionRepository.deleteBySession(session);
            Cookie cookie = new Cookie(COOKIE_NAME, null);
            cookie.setPath("/api");
            cookie.setMaxAge(0);
            resp.addCookie(cookie);
        }
    }

    @Operation(summary = "Change a password", description = "This method is used to change a password.")
    @ApiResponse(responseCode = "200", description = "Password changed")
    @ApiResponse(responseCode = "404", description = "Old password is incorrect.", content = @Content())
    @PostMapping("/password/change")
    public void changePassword(@NotNull @Parameter(hidden = true) User loggedUser, @RequestBody ChangePasswordRequest req) {
        if (req.getOldPassword() == null || req.getOldPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing old password");
        }
        if (req.getNewPassword() == null || req.getNewPassword().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing new password");
        }
        if (req.getNewPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "password too short");
        }
        if (!loggedUser.checkPassword(req.getOldPassword())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "old password is incorrect");
        }
        loggedUser.setPassword(req.getNewPassword());
        userRepository.save(loggedUser);
    }

    @Operation(summary = "Send email with link to reset password", description = "This method sends an e-mail to the user with a link that allows the user to reset the password.")
    @ApiResponse(responseCode = "200", description = "E-mail address has been sent")
    @ApiResponse(responseCode = "403", description = "User already logged")
    @ApiResponse(responseCode = "404", description = "E-mail not found")
    @PostMapping("/password/recover")
    public void recoverPassword(@Parameter(hidden = true) User loggedUser, @RequestBody PasswordRecoveryRequest req) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        if (req.getEmail() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing email");
        }
        User u = userRepository.findByEmail(req.getEmail());
        if (u == null) {
            throw new ObjectNotFoundException();
        }
        PasswordRecovery p = new PasswordRecovery();
        p.setUser(u);
        p.setActive(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
        p.setToken(generateRandomSecureString());
        while (true) {
            try {
                p = passwordRecoveryRepository.save(p);
                break;
            } catch (DataIntegrityViolationException ex) {
                p.setToken(generateRandomSecureString());
            }
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(req.getEmail());
        msg.setFrom("wflow1337@gmail.com");
        msg.setSubject("Zapomniałeś hasła?");
        msg.setText("Cześć, " + u.getName()
                + "!\nJeśli zapomniałeś hasła to wejdź w link: https://workflow.adiantek.ovh/pl-PL/auth/set-new-password?token="
                + p.getToken() + "\nLink wygaśnie po 30 minutach");
        javaMailSender.send(msg);
    }

    @Operation(summary = "Check token and reset password", description = "This method allows to check if the token is valid and reset the password.")
    @ApiResponse(responseCode = "200", description = "The token is valid and the password (if provided) has been changed.")
    @ApiResponse(responseCode = "403", description = "User is already logged.")
    @ApiResponse(responseCode = "404", description = "The token is not valid or has expired.")
    @PostMapping("/password/reset")
    public void resetPassword(@Parameter(hidden = true) User loggedUser, @RequestBody ResetPasswordRequest req) {
        if (loggedUser != null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "already logged");
        }
        if (req.getToken() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing token");
        }
        PasswordRecovery p = passwordRecoveryRepository.findByToken(req.getToken());
        if (p == null) {
            throw new ObjectNotFoundException();
        }
        if (p.getActive().before(new Date())) {
            passwordRecoveryRepository.delete(p);
            throw new ObjectNotFoundException();
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
        us.setSession(generateRandomSecureString());
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
                us.setSession(generateRandomSecureString());
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
    
    @Scheduled(cron = "0 * * * * *")
    public void deleteOldAccount() {
        Date d = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
        List<User> users = this.userRepository.findByDeletedPermanentlyFalseAndDeletedLessThan(d);
        for (User u : users) {
            u.setDeletedPermanently(true);
            u.setUsername("(deleted) " + generateRandomSecureString());
            u.setEmail("(deleted) " + generateRandomSecureString());
        }
        this.userRepository.saveAll(users);
    }
}
