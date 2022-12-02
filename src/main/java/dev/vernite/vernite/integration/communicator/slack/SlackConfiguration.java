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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.slack.api.bolt.App;
import com.slack.api.bolt.AppConfig;
import com.slack.api.bolt.AppConfig.AppConfigBuilder;
import com.slack.api.methods.request.apps.event.authorizations.AppsEventAuthorizationsListRequest;
import com.slack.api.methods.response.apps.event.authorizations.AppsEventAuthorizationsListResponse.Authorization;
import com.slack.api.model.event.MessageEvent;

import dev.vernite.protobuf.CommunicatorModel;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;
import dev.vernite.vernite.ws.SocketHandler;

@Configuration
public class SlackConfiguration {

    @Autowired
    private SlackInstallationRepository repository;

    @Bean
    public App initSlackApp(VerniteInstallationService service, Environment env) {
        AppConfigBuilder builder = AppConfig.builder()
                .signingSecret(env.getProperty("slack.signingSecret"))
                .clientId(env.getProperty("slack.clientId"))
                .clientSecret(env.getProperty("slack.clientSecret"))
                .userScope(env.getProperty("slack.userScope"));
        final App app = new App(builder.build()).service(service).enableTokenRevocationHandlers();

        app.event(MessageEvent.class, (payload, ctx) -> {
            MessageEvent event = payload.getEvent();
            var response = ctx.client().appsEventAuthorizationsList(AppsEventAuthorizationsListRequest.builder()
                    .token(env.getProperty("slack.app.level.token")).eventContext(payload.getEventContext()).build());
            if (!response.isOk()) {
                ctx.logger.error("Cannot get authorizations: {}", response.getError());
                return ctx.ack();
            }
            for (Authorization authorizations : response.getAuthorizations()) {
                repository.findByTeamIdAndInstallerUserId(authorizations.getTeamId(), authorizations.getUserId())
                        .ifPresent(inst -> {
                            CommunicatorModel.Message message = CommunicatorModel.Message
                                    .newBuilder()
                                    .setId(event.getClientMsgId() == null ? "" : event.getClientMsgId())
                                    .setUser(event.getUser())
                                    .setChannel(event.getChannel())
                                    .setContent(event.getText())
                                    // TODO .setTimestamp(event.getTs())
                                    .setProvider("slack")
                                    .build();
                            SocketHandler.sendToUser(inst.getUser().getId(), message);
                        });
            }
            return ctx.ack();
        });

        return app;
    }

    @Bean
    public VerniteInstallationService initVerniteInstallationService(SlackInstallationRepository repository) {
        return new VerniteInstallationService(repository);
    }
}
