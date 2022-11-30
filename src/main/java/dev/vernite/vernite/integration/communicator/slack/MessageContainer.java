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

import java.util.List;

import com.slack.api.methods.response.conversations.ConversationsHistoryResponse;

import dev.vernite.protobuf.CommunicatorModel;
import io.swagger.v3.oas.annotations.media.Schema;

public class MessageContainer {
    @Schema(description = "Null means there is no more messages to load.")
    private String cursor;

    private List<CommunicatorModel.Message> messages;

    public MessageContainer(ConversationsHistoryResponse response) {
        if (response.isHasMore()) {
            this.setCursor(response.getResponseMetadata().getNextCursor());
        }
        this.setMessages(response.getMessages().stream()
                .map(m -> CommunicatorModel.Message.newBuilder()
                        .setId(m.getClientMsgId())
                        .setUser(m.getUser())
                        .setChannel(m.getChannel())
                        .setContent(m.getText())
                        .setTimestamp(m.getTs())
                        .setProvider("slack")
                        .build())
                .toList());
    }

    public String getCursor() {
        return cursor;
    }

    public void setCursor(String cursor) {
        this.cursor = cursor;
    }

    public List<CommunicatorModel.Message> getMessages() {
        return messages;
    }

    public void setMessages(List<CommunicatorModel.Message> messages) {
        this.messages = messages;
    }
}
