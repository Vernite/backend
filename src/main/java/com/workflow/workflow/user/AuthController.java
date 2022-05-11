package com.workflow.workflow.user;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    
    @Autowired
    private UserRepository userRepository;

    @Operation(summary = "Register account.", description = "This method registers a new account. On success returns newly created user.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Newly created user.", content = {
                    @Content(mediaType = "application/json", schema = @Schema(implementation = User.class))
            }),
            @ApiResponse(responseCode = "400", description = "Some fields are missing.", content = @Content()),
            @ApiResponse(responseCode = "422", description = "Username or email is already taken.", content = @Content())
    })
    @PostMapping("/register")
    public User register(@RequestBody RegisterRequest req) {
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
        if (userRepository.findByUsername(req.getUsername()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "this username is already taken");
        }
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "this email is already taken");
        }
        User u = new User();
        u.setEmail(req.getEmail());
        u.setName(req.getName());
        u.setPassword(req.getPassword());
        u.setSurname(req.getSurname());
        u.setUsername(req.getUsername());
        return userRepository.save(u);
    }

	@PostMapping("/logout")
	public void destroySession(HttpServletRequest request) {
		request.getSession().invalidate();
	}
}
