package dev.vernite.vernite.integration.communicator.slack;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;

import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallation;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.utils.SecureStringUtils;
import io.swagger.v3.oas.annotations.Parameter;

@Controller
@RequestMapping("/integration/slack")
public class SlackAuthController {
    private static final StateManager states = new StateManager();
    private static final String FORMAT_URL = "https://vernite.slack.com/oauth?client_id=%s&scope=&user_scope=%s&state=%s&redirect_uri=&granular_bot_scope=1";

    @Autowired
    private App app;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlackInstallationRepository installationRepository;

    @GetMapping("/install")
    public void install(@NotNull @Parameter(hidden = true) User user, HttpServletResponse httpServletResponse)
            throws IOException {
        String userScope = app.config().getUserScope();
        String clientId = app.config().getClientId();
        String state = SecureStringUtils.generateRandomSecureString();
        states.put(state, user.getId());
        httpServletResponse.sendRedirect(
                String.format(FORMAT_URL, clientId, URLEncoder.encode(userScope, StandardCharsets.UTF_8), state));
    }

    @GetMapping("/oauth_redirect")
    public void confirm(String code, String state, HttpServletResponse httpServletResponse)
            throws IOException, SlackApiException {
        Long userId = states.remove(state);
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        User user = userRepository.findById(userId).orElseThrow();
        OAuthV2AccessRequest request = OAuthV2AccessRequest.builder()
                .clientId(app.config().getClientId())
                .clientSecret(app.config().getClientSecret())
                .code(code)
                .build();
        OAuthV2AccessResponse response = app.client().oauthV2Access(request);
        try {
        installationRepository.save(new SlackInstallation(response.getAuthedUser().getAccessToken(),
                response.getAuthedUser().getId(), response.getTeam().getId(), response.getTeam().getName(), user));
        } catch (ConstraintViolationException ex) {
            // TODO: log this or something
        }
        httpServletResponse.sendRedirect("https://vernite.dev");
    }
}
