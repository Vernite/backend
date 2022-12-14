package dev.vernite.vernite.integration.git.github.api;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import lombok.Getter;

/**
 * Bean to store GitHub API configuration.
 */
@Getter
@Component
public class GitHubConfiguration {

    public static final String GITHUB_AUTH_URL = "https://github.com/login/oauth/authorize";

    private final String apiURL;

    private final long appId;

    private final String clientId;

    private final String clientSecret;

    private final Key jwtKey;

    public GitHubConfiguration(Environment env) throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        this.appId = Long.parseLong(env.getProperty("github.app.id"));
        this.clientId = env.getProperty("github.client.id");
        this.clientSecret = env.getProperty("github.client.secret");
        this.apiURL = env.getProperty("github.api.url");

        var path = Path.of(env.getProperty("github.jwt.secret.path"));
        var spec = new PKCS8EncodedKeySpec(Files.readAllBytes(path));
        this.jwtKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
    }

}
