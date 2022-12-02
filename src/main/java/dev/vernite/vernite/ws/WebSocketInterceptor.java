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

import java.net.HttpCookie;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import dev.vernite.vernite.user.UserSession;
import dev.vernite.vernite.user.UserSessionRepository;
import dev.vernite.vernite.user.auth.AuthController;

@Component
public class WebSocketInterceptor implements HandshakeInterceptor {

    private static final Logger L = Logger.getLogger("WebSocketInterceptor");

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            @Nullable Exception exception) {
        if (exception != null) {
            return;
        }
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler wsHandler,
            Map<String, Object> attributes) throws Exception {
        // this list contains separated by space cookies in one string... why?
        List<String> cookie = request.getHeaders().get("cookie");
        if (cookie == null || cookie.isEmpty()) {
            return true;
        }
        for (String c : cookie) {
            for (HttpCookie parsed : HttpCookie.parse(c)) {
                if (parsed.getName().equals(AuthController.COOKIE_NAME)) {
                    Optional<UserSession> session = userSessionRepository.findBySession(parsed.getValue());
                    if (!session.isPresent()) {
                        break;
                    }
                    UserSession us = session.get();
                    us.setIp(request.getHeaders().getFirst("X-Forwarded-For"));
                    if (us.getIp() == null) {
                        us.setIp(request.getRemoteAddress().getAddress().getHostAddress());
                    }
                    us.setLastUsed(new Date());
                    userSessionRepository.save(us);
                    if (!us.getUser().isDeleted()) {
                        L.info("User " + us.getUser().getUsername() + " connected from " + us.getIp());
                        attributes.put("user", us.getUser());
                        return true;
                    }
                }
            }
        }
        return true;
    }
    
}
