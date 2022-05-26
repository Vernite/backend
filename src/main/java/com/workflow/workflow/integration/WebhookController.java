package com.workflow.workflow.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.workflow.workflow.integration.git.github.GitHubWebhookService;
import com.workflow.workflow.integration.git.github.data.GitHubWebhookData;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import io.swagger.v3.oas.annotations.Hidden;
import reactor.core.publisher.Mono;

/**
 * Controller for webhooks from integrated services.
 */
@Hidden
@RestController
@RequestMapping("/webhook")
public class WebhookController {
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    @Autowired
    private GitHubWebhookService gitHubService;

    @PostMapping("/github")
    Mono<Void> github(@RequestHeader("X-Hub-Signature-256") String token, @RequestHeader("X-GitHub-Event") String event,
            @RequestBody String dataRaw) {
        if (!gitHubService.isAuthorized(token, dataRaw)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        GitHubWebhookData data;
        try {
            data = MAPPER.readValue(dataRaw, GitHubWebhookData.class);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST);
        }
        return gitHubService.handleWebhook(event, data);
    }
}
