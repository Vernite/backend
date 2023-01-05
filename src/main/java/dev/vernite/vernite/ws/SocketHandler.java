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

package dev.vernite.vernite.ws;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import dev.vernite.protobuf.KeepAlive;
import dev.vernite.vernite.task.Task;

@Component
public class SocketHandler extends BinaryWebSocketHandler {
    private static final Set<SocketSession> SESSIONS = new CopyOnWriteArraySet<>();
    private static final Map<WebSocketSession, SocketSession> SESSION_MAP = new ConcurrentHashMap<>();
    private static final Map<Long, Set<SocketSession>> SESSIONS_BY_USER = new ConcurrentHashMap<>();

    public static void sendToUser(long userId, Message.Builder message) {
        sendToUser(userId, message.build());
    }

    public static void sendToUser(long userId, Message message) {
        Set<SocketSession> sessions = SESSIONS_BY_USER.get(userId);
        if (sessions != null) {
            bc(sessions, message);
        }
    }

    public static void bc(Message.Builder message) {
        SocketHandler.bc(message.build());
    }

    public static void bc(Message message) {
        SocketHandler.bc(SESSIONS, message);
    }

    private static void bc(Collection<SocketSession> sessions, Message message) {
        for (SocketSession s : sessions) {
            s.send(message);
        }
    }

    public static void bc(Task task, Message.Builder message) {
        SocketHandler.bc(task, message.build());
    }

    public static void bc(Task task, Message message) {
        SocketHandler.bc(task, SESSIONS, message);
    }

    private static void bc(Task task, Collection<SocketSession> sessions, Message message) {
        for (SocketSession s : sessions) {
            if (s.getUser() == null) {
                continue;
            }
            if (task.getSprint().getProject().member(s.getUser()) != -1) {
                s.send(message);
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        SocketSession s = SESSION_MAP.get(session);
        if (s == null) {
            session.close();
            return;
        }
        Any payload = Any.parseFrom(message.getPayload());
        PacketExecutor.call(s, payload);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SocketSession s = SESSION_MAP.get(session);
        if (s != null) {
            SESSIONS.remove(s);
            SESSION_MAP.remove(session);
            if (s.getUser() != null) {
                SESSIONS_BY_USER.get(s.getUser().getId()).remove(s);
            }
            s.close();
        }
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SocketSession s = new SocketSession(session);
        SESSIONS.add(s);
        SESSION_MAP.put(session, s);
        if (s.getUser() != null) {
            SESSIONS_BY_USER.computeIfAbsent(s.getUser().getId(), k -> new CopyOnWriteArraySet<>()).add(s);
        }
    }

    @Scheduled(cron = "* * * * * *")
    public void ping() {
        for (SocketSession s : SESSIONS) {
            s.send(KeepAlive.newBuilder().setId(System.currentTimeMillis()));
        }
    }
}
