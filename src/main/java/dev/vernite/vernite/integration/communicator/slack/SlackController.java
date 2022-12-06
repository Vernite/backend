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

package dev.vernite.vernite.integration.communicator.slack;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotNull;

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
import com.slack.api.methods.request.conversations.ConversationsHistoryRequest;
import com.slack.api.methods.request.conversations.ConversationsInfoRequest;
import com.slack.api.methods.request.conversations.ConversationsListRequest;
import com.slack.api.methods.request.conversations.ConversationsMembersRequest;
import com.slack.api.methods.request.oauth.OAuthV2AccessRequest;
import com.slack.api.methods.request.users.UsersInfoRequest;
import com.slack.api.methods.response.auth.AuthRevokeResponse;
import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;
import com.slack.api.methods.response.conversations.ConversationsListResponse;
import com.slack.api.methods.response.oauth.OAuthV2AccessResponse;
import com.slack.api.methods.response.users.UsersInfoResponse;
import com.slack.api.model.ConversationType;

import dev.vernite.vernite.integration.communicator.model.Channel;
import dev.vernite.vernite.integration.communicator.model.ChatUser;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallation;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;
import dev.vernite.vernite.integration.communicator.slack.model.SlackChannel;
import dev.vernite.vernite.integration.communicator.slack.model.SlackUser;
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
    @ApiResponse(description = "Slack channels", responseCode = "200", content = @Content(array = @ArraySchema(schema = @Schema(implementation = Channel.class))))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Integration with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/slack/{id}/channel")
    public List<Channel> channels(@NotNull @Parameter(hidden = true) User user, @PathVariable long id)
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
        return response.getChannels().stream().map(c -> (Channel) new SlackChannel(c)).toList();
    }

    @Operation(summary = "Get user", description = "Get user info")
    @ApiResponse(description = "Slack user", responseCode = "200", content = @Content(schema = @Schema(implementation = ChatUser.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Integration with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/slack/{id}/user/{userId}")
    public ChatUser getUser(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable String userId) throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        UsersInfoResponse response = app.client()
                .usersInfo(UsersInfoRequest.builder().token(installation.getToken()).user(userId).build());
        if (!response.isOk()) {
            throw new ExternalApiException("slack", "Cannot get user details");
        }
        return new SlackUser(response.getUser());
    }

    @Operation(summary = "Get messages", description = "Get messages for slack channel")
    @ApiResponse(description = "Slack messages", responseCode = "200", content = @Content(schema = @Schema(implementation = MessageContainer.class)))
    @ApiResponse(description = "No user logged in.", responseCode = "401", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @ApiResponse(description = "Integration or channel with given id not found.", responseCode = "404", content = @Content(schema = @Schema(implementation = ErrorType.class)))
    @GetMapping("/user/integration/slack/{id}/channel/{channelId}")
    public MessageContainer messages(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable String channelId, @Parameter(required = false) String cursor)
            throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        ConversationsHistoryResponse response = app.client().conversationsHistory(
                ConversationsHistoryRequest.builder().token(installation.getToken()).channel(channelId).cursor(cursor)
                        .build());
        if (!response.isOk()) {
            throw new ExternalApiException("slack", "Cannot get list of channels");
        }
        return new MessageContainer(response);
    }

    /**
     * Get channel members.
     * 
     * @param user      logged in user
     * @param id        slack integration id
     * @param channelId slack channel id
     * @return list of channel members
     * @throws SlackApiException
     * @throws IOException
     */
    @GetMapping("/user/integration/slack/{id}/channel/{channelId}/members")
    public List<ChatUser> channelMembers(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable String channelId) throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        var response = app.client().conversationsMembers(
                ConversationsMembersRequest.builder().token(installation.getToken()).channel(channelId).build());
        if (!response.isOk()) {
            throw new ExternalApiException("slack", "Cannot get list of channel members");
        }
        return response.getMembers().stream().map(userId -> {
            UsersInfoResponse userResponse;
            try {
                userResponse = app.client()
                        .usersInfo(UsersInfoRequest.builder().token(installation.getToken()).user(userId).build());
            } catch (IOException | SlackApiException e) {
                throw new ExternalApiException("slack", "Cannot get list of channel members");
            }
            if (!userResponse.isOk()) {
                throw new ExternalApiException("slack", "Cannot get user details");
            }
            return (ChatUser) new SlackUser(userResponse.getUser());
        }).toList();
    }

    /**
     * Get channel.
     * 
     * @param user      logged in user
     * @param id        slack integration id
     * @param channelId slack channel id
     * @return slack channel
     * @throws IOException
     * @throws SlackApiException
     */
    @GetMapping("/user/integration/slack/{id}/channel/{channelId}/info")
    public Channel channel(@NotNull @Parameter(hidden = true) User user, @PathVariable long id,
            @PathVariable String channelId)
            throws IOException, SlackApiException {
        SlackInstallation installation = installationRepository.findById(id).orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != user.getId()) {
            throw new ObjectNotFoundException();
        }
        var response = app.client().conversationsInfo(
                ConversationsInfoRequest.builder().token(installation.getToken()).channel(channelId).build());
        if (!response.isOk()) {
            throw new ExternalApiException("slack", "Cannot get list of channels");
        }
        return (Channel) new SlackChannel(response.getChannel());
    }
}
