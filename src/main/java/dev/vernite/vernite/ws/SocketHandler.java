package dev.vernite.vernite.ws;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Logger;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

import com.google.protobuf.Any;
import com.google.protobuf.Message;

import dev.vernite.protobuf.KeepAlive;

@Component
public class SocketHandler extends BinaryWebSocketHandler {
    private static final Logger L = Logger.getLogger("SocketHandler");

    private static final Set<WebSocketSession> SESSIONS = new CopyOnWriteArraySet<>();

    public static void bc(Message message) {
        for (WebSocketSession s : SESSIONS) {
            send(s, message);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        for (WebSocketSession s : SESSIONS) {
            try {
                s.sendMessage(message);
            } catch (Exception e) {
            }
        }
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        Any payload = Any.parseFrom(message.getPayload());
        if (payload.is(KeepAlive.class)) {
            KeepAlive ka = payload.unpack(KeepAlive.class);
            L.info("KeepAlive from " + session.getRemoteAddress() + ": " + (System.currentTimeMillis() - ka.getId()) + " ms");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        SESSIONS.remove(session);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        SESSIONS.add(session);
    }

    
    @Scheduled(cron = "* * * * * *")
    public void ping() {
        for (WebSocketSession s : SESSIONS) {
            send(s, KeepAlive.newBuilder().setId(System.currentTimeMillis()).build());
        }
    }

    public static void send(WebSocketSession session, Message message) {
        try {
            session.sendMessage(new BinaryMessage(Any.pack(message).toByteArray()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
