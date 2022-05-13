package com.workflow.workflow.user;

import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor();

    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Logged user.", description = "This method returns currently logged user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            }),
            @ApiResponse(responseCode = "404", description = "User is not logged.", content = @Content())
    })
    @GetMapping("/me")
    public User me(HttpServletRequest request) {
        HttpSession sess = request.getSession(false);
        if (sess != null) {
            Object id = sess.getAttribute("userID");
            if (id != null) {
                return userRepository.findById((long) id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "user not logged"));
            }
        }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "user not logged");
    }

    @Operation(summary = "Logging in.", description = "This method logs the user in.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            }),
            @ApiResponse(responseCode = "404", description = "Username or password is incorrect.", content = @Content()),
            @ApiResponse(responseCode = "422", description = "Username or email is already taken.", content = @Content())
    })
    @PostMapping("/login")
    public Future<User> login(@RequestBody LoginRequest req, HttpServletRequest request) {
        if (req.getPassword() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing password");
        }
        if (req.getUsername() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "missing username");
        }
        User u = req.getUsername().indexOf('@') != -1 ? userRepository.findByEmail(req.getUsername())
                : userRepository.findByUsername(req.getUsername());
        CompletableFuture<User> f = new CompletableFuture<>();
        EXECUTOR_SERVICE.schedule(() -> {
            if (u == null || !u.checkPassword(req.getPassword())) {
                f.completeExceptionally(new ResponseStatusException(HttpStatus.NOT_FOUND, "username or password incorrect"));
            } else {
                request.getSession(true).setAttribute("userID", u.getId());
                f.complete(u);
            }
        }, 500 + RANDOM.nextInt(500), TimeUnit.MILLISECONDS);
        return f;
    }

    @Operation(summary = "Register account.", description = "This method registers a new account. On success returns newly created user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            }),
            @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content()),
            @ApiResponse(responseCode = "422", description = "Username or email is already taken.", content = @Content())
    })
    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest req, HttpServletRequest request) {
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
        u = userRepository.save(u);
        request.getSession(true).setAttribute("userID", u.getId());
        return u;
    }

    @PostMapping("/logout")
    public void destroySession(HttpServletRequest request) {
        HttpSession sess = request.getSession(false);
        if (sess != null) {
            sess.invalidate();
        }
    }
}
