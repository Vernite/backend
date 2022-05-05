package com.workflow.workflow.integration.git.github.service;

import java.util.List;
import java.util.Optional;

import com.workflow.workflow.integration.git.github.GitHubInstallation;
import com.workflow.workflow.integration.git.github.GitHubInstallationRepository;
import com.workflow.workflow.user.User;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import io.jsonwebtoken.Jwts;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;

@Service
public class GitHubService {
    private static final String APP_ID = "195507";
    private static final WebClient client = WebClient.create();
    private GitHubInstallationRepository installationRepository;

    public GitHubService(GitHubInstallationRepository installationRepository) {
        this.installationRepository = installationRepository;
    }

    public List<GitHubRepository> getRepositories(User user) {
        List<GitHubRepository> repositories = new ArrayList<>();
        Optional<GitHubInstallation> optional = installationRepository.findByUser(user);
        if (optional.isPresent()) {
            GitHubInstallation installation = refreshToken(optional.get());
            GitHubRepositoryList repositoryList = client.get()
                    .uri("https://api.github.com/installation/repositories")
                    .header("Authorization", "Bearer " + installation.getToken())
                    .header("Accept", "application/vnd.github.v3+json")
                    .retrieve()
                    .bodyToMono(GitHubRepositoryList.class)
                    .block();
            if (repositoryList != null) {
                repositories.addAll(repositoryList.getRepositories());
            }
        }
        return repositories;
    }

    private GitHubInstallation refreshToken(GitHubInstallation installation) {
        if (Instant.now().isAfter(installation.getExpiresAt().toInstant())) {
            installation.update(client.post()
                    .uri(String.format("https://api.github.com/app/installations/%d/access_tokens",
                            installation.getInstallationId()))
                    .header("Accept", "application/vnd.github.v3+json")
                    .header("Authorization", "Bearer " + createJWT())
                    .retrieve()
                    .bodyToMono(InstallationToken.class)
                    .block());
        }
        return installationRepository.save(installation);
    }

    private static String createJWT() {
        try {
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(
                    Files.readAllBytes(Path.of("workflow-2022.private-key.der")));
            Key signingKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
            long now = System.currentTimeMillis();
            return Jwts.builder()
                    .setIssuedAt(new Date(now))
                    .setIssuer(APP_ID)
                    .signWith(signingKey)
                    .setExpiration(new Date(now + 600000))
                    .compact();
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Unable to create JWT");
        }
    }
}
