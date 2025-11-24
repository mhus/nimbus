package de.mhus.nimbus.world.provider.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class WorldWebSocketHandler extends TextWebSocketHandler {

    private final AtomicLong counter = new AtomicLong();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri() != null ? session.getUri().getPath() : "";
        String worldId = extractWorldId(path);
        session.sendMessage(new TextMessage("CONNECTED world-provider worldId=" + worldId + " " + Instant.now()));
    }

    private String extractWorldId(String path) {
        // expected /ws/world/{worldId}
        if (path == null) return "";
        String[] parts = path.split("/");
        if (parts.length >= 4) return parts[3];
        return "";
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        long id = counter.incrementAndGet();
        String payload = message.getPayload();
        if ("ping".equalsIgnoreCase(payload)) {
            session.sendMessage(new TextMessage("pong:" + id));
        } else {
            session.sendMessage(new TextMessage("echo:" + id + ":" + payload));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        // no-op
    }
}
