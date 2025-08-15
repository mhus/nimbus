package de.mhus.nimbus.world.bridge.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.worldwebsocket.*;
import de.mhus.nimbus.world.bridge.model.WebSocketSession;
import de.mhus.nimbus.world.bridge.service.WorldBridgeService;
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
        WebSocketSession sessionInfo = sessionData.get(sessionId);

        try {
            // Prüfe, ob die empfangene Zeile leer ist
            if (payload.trim().isEmpty()) {
                // Leere Zeile empfangen - verarbeite den gepufferten Inhalt
                if (sessionInfo.hasMessageBuffer()) {
                    String bufferedMessage = sessionInfo.getMessageBuffer();
                    sessionInfo.clearMessageBuffer();
                    processJsonCommand(session, sessionId, bufferedMessage);
                }
                return;
            }

            // Prüfe, ob die erste Zeile bereits ein valides JSON ist
            if (!sessionInfo.hasMessageBuffer()) {
                try {
                    WorldWebSocketCommand command = objectMapper.readValue(payload, WorldWebSocketCommand.class);
                    // Valides JSON gefunden - sofort verarbeiten
                    WorldWebSocketResponse response = processCommand(sessionId, command);
                    sendResponse(session, response);
                    return;
                } catch (Exception e) {
                    // Kein valides JSON - zur Multi-Line-Verarbeitung übergehen
                }
            }

            // Füge die Zeile zum Buffer hinzu
            sessionInfo.appendToMessageBuffer(payload);

        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            // Buffer leeren bei Fehler
            sessionInfo.clearMessageBuffer();
            WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                    .status("error")
                    .errorCode("INVALID_MESSAGE")
                    .message("Invalid message format")
                    .build();
            sendResponse(session, errorResponse);
        }
    }

    private void processJsonCommand(org.springframework.web.socket.WebSocketSession session, String sessionId, String jsonPayload) {
        try {
            WorldWebSocketCommand command = objectMapper.readValue(jsonPayload, WorldWebSocketCommand.class);
            WorldWebSocketResponse response = processCommand(sessionId, command);
            sendResponse(session, response);
        } catch (Exception e) {
            log.error("Error processing buffered WebSocket command", e);
            WorldWebSocketResponse errorResponse = WorldWebSocketResponse.builder()
                    .status("error")
                    .errorCode("INVALID_JSON")
                    .message("Invalid JSON format in multi-line command")
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

    private WorldWebSocketResponse processCommand(String sessionId, WorldWebSocketCommand command) {
        WebSocketSession sessionInfo = sessionData.get(sessionId);

        if (!"bridge".equals(command.getService())) {
            return WorldWebSocketResponse.builder()
                    .service(command.getService())
                    .command(command.getCommand())
                    .requestId(command.getRequestId())
                    .status("error")
                    .errorCode("INVALID_SERVICE")
                    .message("Service not supported by bridge")
                    .build();
        }

        return worldBridgeService.processCommand(sessionId, sessionInfo, command);
    }

    private void sendResponse(org.springframework.web.socket.WebSocketSession session, WorldWebSocketResponse response) {
        try {
            String responseJson = objectMapper.writeValueAsString(response);
            session.sendMessage(new TextMessage(responseJson));
        } catch (IOException e) {
            log.error("Error sending WebSocket response", e);
        }
    }

    public void sendMessageToSession(String sessionId, WorldWebSocketResponse message) {
        org.springframework.web.socket.WebSocketSession session = sessions.get(sessionId);
        if (session != null && session.isOpen()) {
            sendResponse(session, message);
        }
    }

    public ConcurrentHashMap<String, WebSocketSession> getSessionData() {
        return sessionData;
    }
}
