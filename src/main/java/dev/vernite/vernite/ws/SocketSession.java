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

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import dev.vernite.vernite.user.User;

public class SocketSession implements Closeable {

    private static final AtomicLong ID = new AtomicLong();

    private final WebSocketSession session;
    private boolean closed = false;
    private final long id;
    private final String ip;
    private final User user;

    public SocketSession(WebSocketSession session) {
        this.id = ID.incrementAndGet();
        this.session = session;
        this.ip = session.getHandshakeHeaders().getFirst("X-Forwarded-For") != null
                ? session.getHandshakeHeaders().getFirst("X-Forwarded-For")
                : Objects.toString(session.getRemoteAddress());
        this.user = (User) session.getAttributes().get("user");
    }

    public User getUser() {
        return this.user;
    }

    public void send(Message.Builder message) {
        send(message.build());
    }

    public void send(Message message) {
        if (closed) {
            return;
        }
        try {
            session.sendMessage(new BinaryMessage(Any.pack(message, "").toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
            try {
                session.close();
            } catch (IOException e1) {
            }
        }
    }

    @Override
    public void close() {
        this.closed = true;
    }

    @Override
    public String toString() {
        return "[id=" + id + ", ip=" + ip + ", " + user + "]";
    }

}
