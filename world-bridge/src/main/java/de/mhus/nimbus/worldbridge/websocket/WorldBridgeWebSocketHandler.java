package de.mhus.nimbus.worldbridge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.websocket.*;
import de.mhus.nimbus.worldbridge.model.WebSocketSession;
import de.mhus.nimbus.worldbridge.service.WorldBridgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.*;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorldBridgeWebSocketHandler implements WebSocketHandler {

    private final WorldBridgeService worldBridgeService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ConcurrentHashMap<String, org.springframework.web.socket.WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, WebSocketSession> sessionData = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(org.springframework.web.socket.WebSocketSession session) {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        sessionData.put(sessionId, new WebSocketSession());
        log.info("WebSocket connection established: {}", sessionId);
    }

    @Override
    public void handleMessage(org.springframework.web.socket.WebSocketSession session, WebSocketMessage<?> message) {
        String sessionId = session.getId();
        String payload = (String) message.getPayload();

        try {
            WebSocketCommand command = objectMapper.readValue(payload, WebSocketCommand.class);
            WebSocketResponse response = processCommand(sessionId, command);
            sendResponse(session, response);
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            WebSocketResponse errorResponse = WebSocketResponse.builder()
                    .status("error")
                    .errorCode("INVALID_MESSAGE")
                    .message("Invalid message format")
                    .build();
            sendResponse(session, errorResponse);
        }
    }

    @Override
    public void handleTransportError(org.springframework.web.socket.WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error for session {}", session.getId(), exception);
    }

    @Override
    public void afterConnectionClosed(org.springframework.web.socket.WebSocketSession session, CloseStatus closeStatus) {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        sessionData.remove(sessionId);
        log.info("WebSocket connection closed: {} with status: {}", sessionId, closeStatus);
    }

    @Override
    public boolean supportsPartialMessages() {
        return false;
    }

    private WebSocketResponse processCommand(String sessionId, WebSocketCommand command) {
        WebSocketSession sessionInfo = sessionData.get(sessionId);

        if (!"bridge".equals(command.getService())) {
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("INVALID_SERVICE")
                    .message("Service not supported by bridge")
                    .build();
        }

        // Check authentication for all commands except login
        if (!"login".equals(command.getCommand()) && !sessionInfo.isLoggedIn()) {
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("NOT_AUTHENTICATED")
                    .message("User not authenticated")
                    .build();
        }

        // Check world selection for all commands except login and use
        if (!"login".equals(command.getCommand()) && !"use".equals(command.getCommand())
            && !"ping".equals(command.getCommand()) && !sessionInfo.hasWorld()) {
            return WebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("NO_WORLD_SELECTED")
                    .message("No world selected")
                    .build();
        }

        return worldBridgeService.processCommand(sessionId, sessionInfo, command);
    }

    private void sendResponse(org.springframework.web.socket.WebSocketSession session, WebSocketResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));
        } catch (IOException e) {
            log.error("Error sending WebSocket response", e);
        }
    }

    public void sendMessageToSession(String sessionId, WebSocketResponse message) {
        org.springframework.web.socket.WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendResponse(session, message);
        }
    }

    public ConcurrentHashMap<String, WebSocketSession> getSessionData() {
        return sessionData;
    }
}
