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

package dev.vernite.vernite.ws.packets;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.google.protobuf.Any;
import com.google.protobuf.InvalidProtocolBufferException;
import com.slack.api.bolt.App;
import com.slack.api.methods.SlackApiException;
import com.slack.api.methods.request.chat.ChatPostMessageRequest;

import dev.vernite.protobuf.CommunicatorModel.SendMessage;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallation;
import dev.vernite.vernite.integration.communicator.slack.entity.SlackInstallationRepository;
import dev.vernite.vernite.utils.ObjectNotFoundException;
import dev.vernite.vernite.ws.IHandler;
import dev.vernite.vernite.ws.SocketSession;

@Component
public class SendMessageHandler implements IHandler<SendMessage>, ApplicationContextAware {

    private static final Logger L = Logger.getLogger("SendMessageHandler");

    // Autowired XD
    private static App app;
    private static SlackInstallationRepository slackInstallationRepository;

    @Override
    public void handle(SocketSession session, SendMessage packet) {
        if (session.getUser() == null) {
            L.warning(session + ": User not logged in");
            return;
        }
        if (!packet.getProvider().equals("slack")) {
            L.warning(session + ": Unsupported provider " + packet.getProvider());
            return;
        }
        // TODO exception handling?
        SlackInstallation installation = slackInstallationRepository.findById(packet.getIntegrationID())
                .orElseThrow(ObjectNotFoundException::new);
        if (installation.getUser().getId() != session.getUser().getId()) {
            throw new ObjectNotFoundException();
        }
        try {
            ChatPostMessageRequest req = ChatPostMessageRequest.builder()
                    .token(installation.getToken())
                    .channel(packet.getChannel())
                    .text(packet.getContent())
                    .build();
            System.out.println(app.client().chatPostMessage(req));
        } catch (IOException | SlackApiException e) {
            e.printStackTrace();
        }
        L.info("done?");
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        SendMessageHandler.app = applicationContext.getBean(App.class);
        SendMessageHandler.slackInstallationRepository = applicationContext.getBean(SlackInstallationRepository.class);
    }

    // public static void main(String[] args) throws InvalidProtocolBufferException {
    //     byte[] b = Any.pack(SendMessage.newBuilder().setChannel("D04CLSG3C5N").setContent("pozdrawiam ze springa")
    //                 .setIntegrationID(9L)
    //             .setProvider("slack").build()).toByteArray();
    //     System.out.println(Arrays.toString(b));
    // }
}
