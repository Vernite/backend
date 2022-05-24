package com.workflow.workflow.user;

import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

@RestController
@RequestMapping("/session")
public class SessionController {
    
    @Autowired
    private UserSessionRepository userSessionRepository;
    
    @Operation(summary = "List all active sessions", description = "This method returns array of all sessions. Result can be empty array.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of all active sessions. Can be empty.", content = {
                    @Content(mediaType = "application/json", array = @ArraySchema(schema = @Schema(implementation = UserSession.class)))
            })
    })
    @GetMapping
    public Iterable<UserSession> all(@NotNull @Parameter(hidden = true) User loggedUser) {
        return userSessionRepository.findByUser(loggedUser);
    }
}
