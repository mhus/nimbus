package de.mhus.nimbus.entrance.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.common.service.SecurityService;
import de.mhus.nimbus.entrance.service.ClientSessionService;
import de.mhus.nimbus.entrance.service.MessageDispatcher;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import de.mhus.nimbus.shared.dto.AuthenticationRequest;
import de.mhus.nimbus.shared.dto.AuthenticationResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * WebSocket Handler für Client-Verbindungen
 * Verwaltet Authentifizierung und Nachrichtendispatching
 */
@Component
@Slf4j
public class NimbusWebSocketHandler extends TextWebSocketHandler {

    private final SecurityService securityService;
    private final ClientSessionService clientSessionService;
    private final MessageDispatcher messageDispatcher;
    private final ObjectMapper objectMapper;

    public NimbusWebSocketHandler(SecurityService securityService,
                                 ClientSessionService clientSessionService,
                                 MessageDispatcher messageDispatcher,
                                 ObjectMapper objectMapper) {
        this.securityService = securityService;
        this.clientSessionService = clientSessionService;
        this.messageDispatcher = messageDispatcher;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        LOGGER.info("WebSocket Verbindung hergestellt: {}", session.getId());
        clientSessionService.addSession(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            LOGGER.debug("Nachricht empfangen von {}: {}", session.getId(), payload);

            JsonNode jsonNode = objectMapper.readTree(payload);
            WebSocketMessage wsMessage = objectMapper.treeToValue(jsonNode, WebSocketMessage.class);

            if ("authenticate".equals(wsMessage.getType())) {
                handleAuthentication(session, wsMessage);
            } else {
                // Prüfe Authentifizierung für alle anderen Nachrichten
                if (!clientSessionService.isAuthenticated(session.getId())) {
                    sendErrorMessage(session, "Nicht authentifiziert", null);
                    return;
                }

                // Weiterleitung an MessageDispatcher
                messageDispatcher.dispatch(session, wsMessage);
            }
        } catch (Exception e) {
            LOGGER.error("Fehler beim Verarbeiten der Nachricht von {}: {}", session.getId(), e.getMessage(), e);
            sendErrorMessage(session, "Fehler beim Verarbeiten der Nachricht", null);
        }
    }

    private void handleAuthentication(WebSocketSession session, WebSocketMessage message) throws Exception {
        try {
            AuthenticationRequest authRequest = objectMapper.convertValue(message.getData(), AuthenticationRequest.class);

            // Authentifizierung über SecurityService
            SecurityService.LoginResult loginResult = securityService.login(
                authRequest.getUsername(),
                authRequest.getPassword()
            );

            if (loginResult.isSuccess()) {
                // Session als authentifiziert markieren
                clientSessionService.authenticateSession(session.getId(), loginResult.getToken(), authRequest.getUsername());

                AuthenticationResponse response = AuthenticationResponse.builder()
                    .success(true)
                    .token(loginResult.getToken())
                    .message("Authentifizierung erfolgreich")
                    .build();

                WebSocketMessage responseMessage = WebSocketMessage.builder()
                    .type("authentication_response")
                    .requestId(message.getRequestId())
                    .data(response)
                    .build();

                sendMessage(session, responseMessage);
                LOGGER.info("Client {} erfolgreich authentifiziert als {}", session.getId(), authRequest.getUsername());
            } else {
                AuthenticationResponse response = AuthenticationResponse.builder()
                    .success(false)
                    .message("Authentifizierung fehlgeschlagen")
                    .build();

                WebSocketMessage responseMessage = WebSocketMessage.builder()
                    .type("authentication_response")
                    .requestId(message.getRequestId())
                    .data(response)
                    .build();

                sendMessage(session, responseMessage);
                LOGGER.warn("Authentifizierung fehlgeschlagen für Client {}", session.getId());
            }
        } catch (Exception e) {
            LOGGER.error("Fehler bei Authentifizierung für {}: {}", session.getId(), e.getMessage(), e);
            sendErrorMessage(session, "Authentifizierungsfehler", message.getRequestId());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        LOGGER.info("WebSocket Verbindung geschlossen: {} (Status: {})", session.getId(), status);
        clientSessionService.removeSession(session.getId());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        LOGGER.error("Transport Fehler für {}: {}", session.getId(), exception.getMessage(), exception);
        clientSessionService.removeSession(session.getId());
    }

    private void sendMessage(WebSocketSession session, WebSocketMessage message) throws Exception {
        String json = objectMapper.writeValueAsString(message);
        session.sendMessage(new TextMessage(json));
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage, String requestId) {
        try {
            WebSocketMessage errorResponse = WebSocketMessage.builder()
                .type("error")
                .requestId(requestId)
                .data(errorMessage)
                .build();

            sendMessage(session, errorResponse);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der Fehlernachricht: {}", e.getMessage(), e);
        }
    }
}
