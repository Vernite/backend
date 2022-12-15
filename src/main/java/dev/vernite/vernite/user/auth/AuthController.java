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

package dev.vernite.vernite.user.auth;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.vernite.vernite.common.utils.counter.CounterSequence;
import dev.vernite.vernite.event.Event;
import dev.vernite.vernite.event.EventFilter;
import dev.vernite.vernite.event.EventService;
import dev.vernite.vernite.integration.calendar.CalendarIntegration;
import dev.vernite.vernite.integration.calendar.CalendarIntegrationRepository;
import dev.vernite.vernite.task.time.TimeTrack;
import dev.vernite.vernite.task.time.TimeTrackRepository;
import dev.vernite.vernite.user.DeleteAccountRequest;
import dev.vernite.vernite.user.DeleteAccountRequestRepository;
import dev.vernite.vernite.user.PasswordRecovery;
import dev.vernite.vernite.user.PasswordRecoveryRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import dev.vernite.vernite.utils.SecureStringUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;
import lombok.Setter;

@RestController
@RequestMapping("/auth")
public class AuthController {

    public static final String COOKIE_NAME = "session";
    private static final URI RECAPTCHA_URI;
    static {
        try {
            RECAPTCHA_URI = new URI("https://www.google.com/recaptcha/api/siteverify");
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    @Autowired
    private EventService eventService;

    @Autowired
    private CalendarIntegrationRepository calendarRepository;

    @Setter
    @Value("${server.servlet.context-path}")
    private String cookiePath;

    @Value("${recaptcha.secret}")
    private String recaptchaSecret;

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

    @Operation(summary = "Get user events", description = "This method gets events for logged in user. `From` and `to` are required timestamps.")
    @ApiResponse(responseCode = "200", description = "List with events for current user. Empty list if no events. Tasks are only displayed if they are not finished and assigned to user.")
    @ApiResponse(responseCode = "401", description = "User is not logged.", content = @Content())
    @GetMapping("/me/events")
    public Set<Event> getEvents(@NotNull @Parameter(hidden = true) User loggedUser, long from, long to,
            @ModelAttribute EventFilter filter) {
        return eventService.getUserEvents(loggedUser, new Date(from), new Date(to), filter);
    }

    @Operation(summary = "Create synchronization link", description = "Creates synchronization link for user events calendar")
    @ApiResponse(description = "Link.", responseCode = "200")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @PostMapping("/me/events/sync")
    public String createCalendarSync(@NotNull @Parameter(hidden = true) User loggedUser) {
        String key = SecureStringUtils.generateRandomSecureString();
        while (calendarRepository.findByKey(key).isPresent()) {
            key = SecureStringUtils.generateRandomSecureString();
        }
        Optional<CalendarIntegration> integration = calendarRepository.findByUserAndProjectNull(loggedUser);
        if (integration.isPresent()) {
            key = integration.get().getKey();
        } else {
            calendarRepository.save(new CalendarIntegration(loggedUser, key));
        }
        return "https://vernite.dev/api/webhook/calendar?key=" + key;
    }

    @Operation(summary = "Delete account", description = "This method deletes currently logged user by sending an e-mail with a confirmation link.")
    @ApiResponse(responseCode = "200")
    @DeleteMapping("/delete")
    public void delete(@NotNull @Parameter(hidden = true) User loggedUser) {
        DeleteAccountRequest d = new DeleteAccountRequest();
        d.setUser(loggedUser);
        d.setActive(Date.from(Instant.now().plus(30, ChronoUnit.MINUTES)));
        d.setToken(SecureStringUtils.generateRandomSecureString());
        while (true) {
            try {
                d = deleteAccountRepository.save(d);
                break;
            } catch (DataIntegrityViolationException ex) {
                d.setToken(SecureStringUtils.generateRandomSecureString());
            }
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(loggedUser.getEmail());
        message.setSubject("Potwierdzenie usunięcia Twojego konta");
        message.setText("Aby potwierdzić usuwanie Twojego konta, kliknij w poniższy link:\n" +
                "https://vernite.dev/pl-PL/auth/delete-account?token=" + d.getToken() + "\n" +
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
    @ApiResponse(responseCode = "403", description = "User is already logged or invalid captcha.", content = @Content())
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
        return verifyCaptcha(req.getCaptcha(), request, "login").thenApply(success -> {
            if (!success) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid captcha");
            }
            if (req.getEmail().indexOf('@') != -1) {
                return userRepository.findByEmail(req.getEmail());
            }
            return userRepository.findByUsername(req.getEmail());
        }).thenCompose(u -> {
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
        });
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
    @ApiResponse(responseCode = "403", description = "User is already logged or invalid captcha.", content = @Content())
    @ApiResponse(responseCode = "422", description = "Username or email is already taken.", content = @Content())
    @PostMapping("/register")
    public Future<User> register(@Parameter(hidden = true) User loggedUser, @RequestBody RegisterRequest req,
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
        return verifyCaptcha(req.getCaptcha(), request, "register").thenApply(success -> {
            if (!success) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "invalid captcha");
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
            msg.setFrom("contact@vernite.dev");
            // TODO activation link
            msg.setSubject("Dziękujemy za rejestrację");
            msg.setText("Cześć, " + req.getName() + "!\nDziękujemy za zarejestrowanie się w naszym serwisie");
            javaMailSender.send(msg);
            return u;
        });
    }

    @Operation(summary = "Log out", description = "This method log outs the user.")
    @ApiResponse(responseCode = "200", description = "User logged out")
    @PostMapping("/logout")
    public void destroySession(HttpServletRequest req, HttpServletResponse resp,
            @Parameter(hidden = true) @CookieValue(AuthController.COOKIE_NAME) String session) {
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
    public void changePassword(@NotNull @Parameter(hidden = true) User loggedUser,
            @RequestBody ChangePasswordRequest req) {
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
        p.setToken(SecureStringUtils.generateRandomSecureString());
        while (true) {
            try {
                p = passwordRecoveryRepository.save(p);
                break;
            } catch (DataIntegrityViolationException ex) {
                p.setToken(SecureStringUtils.generateRandomSecureString());
            }
        }
        SimpleMailMessage msg = new SimpleMailMessage();
        msg.setTo(req.getEmail());
        msg.setFrom("contact@vernite.dev");
        msg.setSubject("Zapomniałeś hasła?");
        msg.setText("Cześć, " + u.getName()
                + "!\nJeśli zapomniałeś hasła to wejdź w link: https://vernite.dev/pl-PL/auth/set-new-password?token="
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
        us.setSession(SecureStringUtils.generateRandomSecureString());
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
                us.setSession(SecureStringUtils.generateRandomSecureString());
            }
        }
        Cookie c = new Cookie(COOKIE_NAME, us.getSession());
        c.setPath(cookiePath);
        if (req.getHeader("X-Forwarded-For") != null) {
            c.setSecure(true);
        } else {
            c.setHttpOnly(true);
        }
        resp.addCookie(c);
    }

    /**
     * Verify captcha response
     * 
     * @param response response from recaptcha
     * @param request request
     * @param expectedAction expected action
     * @return action if success or null if failed
     */
    private CompletableFuture<Boolean> verifyCaptcha(String response, HttpServletRequest request, String expectedAction) {
        String remoteip = request.getHeader("X-Forwarded-For");
        if (remoteip == null) {
            remoteip = request.getRemoteAddr();
        }

        HttpClient client = HttpClient.newHttpClient();
        String data = String.format("secret=%s&response=%s&remoteip=%s",
                URLEncoder.encode(recaptchaSecret, StandardCharsets.UTF_8),
                URLEncoder.encode(response, StandardCharsets.UTF_8),
                URLEncoder.encode(remoteip, StandardCharsets.UTF_8));
        
        HttpRequest req = HttpRequest.newBuilder(RECAPTCHA_URI)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(data))
                .build();
        return client.sendAsync(req, BodyHandlers.ofString()).thenApply(n -> {
            if (n.statusCode() != 200) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Captcha verification failed");
            }
            try {
                JsonNode node = MAPPER.readTree(n.body());
                if (node.get("success").asBoolean()) {
                    if (node.has("action") && expectedAction.equals(node.get("action").asText())) {
                        return true;
                    }
                    if (node.has("hostname") && "testkey.google.com".equals(node.get("hostname").asText())) {
                        return true;
                    }
                    return false;
                }
            } catch (JsonProcessingException e) {
                e.printStackTrace();
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Captcha verification failed");
            }
            return false;
        });
    }

    @Scheduled(cron = "0 * * * * *")
    public void deleteOldAccount() {
        Date d = Date.from(Instant.now().minus(7, ChronoUnit.DAYS));
        List<User> users = this.userRepository.findByDeletedPermanentlyFalseAndDeletedLessThan(d);
        for (User u : users) {
            u.setDeletedPermanently(true);
            u.setUsername("(deleted) " + SecureStringUtils.generateRandomSecureString());
            u.setEmail("(deleted) " + SecureStringUtils.generateRandomSecureString());
        }
        this.userRepository.saveAll(users);
    }
}
