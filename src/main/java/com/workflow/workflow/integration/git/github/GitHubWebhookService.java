package com.workflow.workflow.integration.git.github;

import com.workflow.workflow.integration.git.github.data.GitHubWebhookData;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

@Service
public class GitHubWebhookService {

    public boolean isAuthorized(String token, String dataRaw) {
        return false;
    }

    public Mono<Void> handleWebhook(String event, GitHubWebhookData data) {
        return null;
    }
    
}
