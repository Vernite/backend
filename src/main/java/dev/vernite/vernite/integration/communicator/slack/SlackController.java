package dev.vernite.vernite.integration.communicator.slack;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.auth.AuthRevokeRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.response.auth.AuthRevokeResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.model.Conversation;
import com.slack.api.model.ConversationType;

import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallation;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;
import dev.vernite.vernite.user.User;
import dev.vernite.vernite.user.UserRepository;
import dev.vernite.vernite.utils.ErrorType;
import dev.vernite.vernite.utils.ExternalApiException;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import dev.vernite.vernite.utils.SecureStringUtils;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
public class SlackController {
    private static final StateManager states = new StateManager();
    private static final String FORMAT_URL = "https://vernite.slack.com/oauth?client_id=%s&scope=&user_scope=%s&state=%s&redirect_uri=&granular_bot_scope=1";

    @Autowired
    private App app;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SlackInstallationRepository installationRepository;

    @Operation(summary = "Install slack", description = "This link redirects user to slack. After installation user will be redirected to https://vernite.dev/slack")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/integration/slack/install")
    public void install(@NotNull @Parameter(hidden = true) User user, HttpServletResponse httpServletResponse)
            throws IOException {
        String userScope = app.config().getUserScope();
        String clientId = app.config().getClientId();
        String state = SecureStringUtils.generateRandomSecureString();
        states.put(state, user.getId());
        httpServletResponse.sendRedirect(
                String.format(FORMAT_URL, clientId, URLEncoder.encode(userScope, StandardCharsets.UTF_8), state));
    }

    @Hidden
    @GetMapping("/integration/slack/oauth_redirect")
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
        } catch (Exception ex) {
            // TODO: log this or something
        }
        httpServletResponse.sendRedirect("https://vernite.dev/slack");
    }

    @Operation(summary = "Get slack integrations", description = "Gets all slack integrations for given user")
    @GetMapping("/user/integration/slack")
    public List<SlackInstallation> getInstallation(@NotNull @Parameter(hidden = true) User user) {
        return installationRepository.findByUser(user);
    }

    @Operation(summary = "Delete slack integration", description = "Delete slack integration with given id")
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Integration with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @DeleteMapping("/user/integration/slack/{id}")
    public void deleteInstallation(@NotNull @Parameter(hidden = true) User user, @PathVariable long id)
            throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        AuthRevokeResponse response = app.client()
                .authRevoke(AuthRevokeRequest.builder().token(installation.getToken()).build());
        if (!response.isOk()) {
            // TODO: log this
        }
        installationRepository.delete(installation);
    }

    @Operation(summary = "Get channels", description = "Get channels for slack integration")
    @ApiResponse(description = "Slack channels", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Conversation.class))))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Integration with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/slack/{id}/channels")
    public List<Conversation> channels(@NotNull @Parameter(hidden = true) User user, @PathVariable long id)
            throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        ConversationsListResponse response = app.client()
                .conversationsList(ConversationsListRequest.builder().token(installation.getToken())
                        .types(List.of(ConversationType.PRIVATE_CHANNEL, ConversationType.PUBLIC_CHANNEL,
                                ConversationType.IM, ConversationType.MPIM))
                        .build());
        if (!response.isOk()) {
            throw new ExternalApiException("slack", "Cannot get list of channels");
        }
        return response.getChannels();
    }
}
