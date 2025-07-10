package de.mhus.nimbus.client.common.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * WebSocket-Client für die Verbindung zum Nimbus Entrance Server
 */
@Slf4j
public class NimbusWebSocketClient extends WebSocketClient {

    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, CompletableFuture<WebSocketMessage>> pendingRequests;
    private final ConcurrentHashMap<String, Consumer<WebSocketMessage>> messageHandlers;

    @Getter
    private boolean authenticated = false;

    public NimbusWebSocketClient(URI serverUri) {
        super(serverUri);
        this.objectMapper = new ObjectMapper();
        this.pendingRequests = new ConcurrentHashMap<>();
        this.messageHandlers = new ConcurrentHashMap<>();
    }

    @Override
    public void onOpen(ServerHandshake handshake) {
        LOGGER.info("WebSocket-Verbindung zum Nimbus Server hergestellt: {}", getURI());
    }

    @Override
    public void onMessage(String message) {
        try {
            WebSocketMessage wsMessage = objectMapper.readValue(message, WebSocketMessage.class);
            LOGGER.debug("Nachricht empfangen: {}", wsMessage);

            // Prüfe ob es eine Antwort auf eine ausstehende Anfrage ist
            if (wsMessage.getRequestId() != null) {
                CompletableFuture<WebSocketMessage> future = pendingRequests.remove(wsMessage.getRequestId());
                if (future != null) {
                    future.complete(wsMessage);
                    return;
                }
            }

            // Prüfe ob es einen spezifischen Handler für den Nachrichtentyp gibt
            Consumer<WebSocketMessage> handler = messageHandlers.get(wsMessage.getType());
            if (handler != null) {
                handler.accept(wsMessage);
            } else {
                LOGGER.warn("Kein Handler für Nachrichtentyp '{}' gefunden", wsMessage.getType());
            }

        } catch (Exception e) {
            LOGGER.error("Fehler beim Verarbeiten der WebSocket-Nachricht: {}", message, e);
        }
    }

    @Override
    public void onClose(int code, String reason, boolean remote) {
        LOGGER.info("WebSocket-Verbindung geschlossen. Code: {}, Grund: {}, Remote: {}", code, reason, remote);
        authenticated = false;

        // Alle ausstehenden Anfragen mit Fehler abschließen
        pendingRequests.values().forEach(future ->
            future.completeExceptionally(new RuntimeException("Verbindung geschlossen"))
        );
        pendingRequests.clear();
    }

    @Override
    public void onError(Exception ex) {
        LOGGER.error("WebSocket-Fehler aufgetreten", ex);
    }

    /**
     * Sendet eine WebSocket-Nachricht und wartet auf eine Antwort
     */
    public CompletableFuture<WebSocketMessage> sendRequest(WebSocketMessage message) {
        if (!isOpen()) {
            return CompletableFuture.failedFuture(new RuntimeException("WebSocket-Verbindung nicht offen"));
        }

        CompletableFuture<WebSocketMessage> future = new CompletableFuture<>();

        if (message.getRequestId() != null) {
            pendingRequests.put(message.getRequestId(), future);

            // Timeout nach 30 Sekunden
            future.orTimeout(30, TimeUnit.SECONDS)
                  .whenComplete((result, throwable) -> {
                      if (throwable != null) {
                          pendingRequests.remove(message.getRequestId());
                      }
                  });
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            send(json);
            LOGGER.debug("Nachricht gesendet: {}", message);

            if (message.getRequestId() == null) {
                future.complete(null); // Für Fire-and-Forget Nachrichten
            }
        } catch (Exception e) {
            pendingRequests.remove(message.getRequestId());
            future.completeExceptionally(e);
        }

        return future;
    }

    /**
     * Sendet eine WebSocket-Nachricht ohne auf Antwort zu warten
     */
    public void sendMessage(WebSocketMessage message) {
        if (!isOpen()) {
            LOGGER.warn("Kann Nachricht nicht senden - WebSocket-Verbindung nicht offen");
            return;
        }

        try {
            String json = objectMapper.writeValueAsString(message);
            send(json);
            LOGGER.debug("Fire-and-Forget Nachricht gesendet: {}", message);
        } catch (Exception e) {
            LOGGER.error("Fehler beim Senden der Nachricht", e);
        }
    }

    /**
     * Registriert einen Handler für einen bestimmten Nachrichtentyp
     */
    public void registerMessageHandler(String messageType, Consumer<WebSocketMessage> handler) {
        messageHandlers.put(messageType, handler);
        LOGGER.debug("Handler für Nachrichtentyp '{}' registriert", messageType);
    }

    /**
     * Entfernt einen Handler für einen bestimmten Nachrichtentyp
     */
    public void unregisterMessageHandler(String messageType) {
        messageHandlers.remove(messageType);
        LOGGER.debug("Handler für Nachrichtentyp '{}' entfernt", messageType);
    }

    /**
     * Setzt den Authentifizierungsstatus
     */
    public void setAuthenticated(boolean authenticated) {
        this.authenticated = authenticated;
        LOGGER.info("Authentifizierungsstatus gesetzt: {}", authenticated);
    }
}
