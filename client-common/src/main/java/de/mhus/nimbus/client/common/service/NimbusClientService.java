package de.mhus.nimbus.client.common.service;

import de.mhus.nimbus.client.common.websocket.NimbusWebSocketClient;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Client-Service Bean für WebSocket-Verbindungen zum Nimbus Entrance Server
 * Diese Bean kann ohne Spring verwendet werden und stellt WebSocket-Funktionalität bereit
 */
@Slf4j
public class NimbusClientService {

    @Getter
    private NimbusWebSocketClient webSocketClient;

    @Getter
    private String serverUrl;

    @Getter
    private boolean connected = false;

    /**
     * Erstellt eine neue Instanz des Client-Services
     */
    public NimbusClientService() {
        log.info("NimbusClientService initialisiert");
    }

    /**
     * Verbindet sich mit dem Nimbus WebSocket Server
     *
     * @param serverUrl URL des WebSocket Servers (z.B. "ws://localhost:8080/nimbus")
     * @return CompletableFuture das abgeschlossen wird wenn die Verbindung hergestellt ist
     */
    public CompletableFuture<Void> connect(String serverUrl) {
        this.serverUrl = serverUrl;

        try {
            URI serverUri = URI.create(serverUrl);
            webSocketClient = new NimbusWebSocketClient(serverUri);

            CompletableFuture<Void> connectionFuture = new CompletableFuture<>();

            // Override onOpen um das Future zu komplettieren
            webSocketClient = new NimbusWebSocketClient(serverUri) {
                @Override
                public void onOpen(org.java_websocket.handshake.ServerHandshake handshake) {
                    super.onOpen(handshake);
                    connected = true;
                    connectionFuture.complete(null);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    super.onClose(code, reason, remote);
                    connected = false;
                }

                @Override
                public void onError(Exception ex) {
                    super.onError(ex);
                    if (!connectionFuture.isDone()) {
                        connectionFuture.completeExceptionally(ex);
                    }
                }
            };

            webSocketClient.connect();
            log.info("Verbindungsaufbau zu {} gestartet", serverUrl);

            return connectionFuture;

        } catch (Exception e) {
            log.error("Fehler beim Verbindungsaufbau zu {}", serverUrl, e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Schließt die WebSocket-Verbindung
     */
    public void disconnect() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
            log.info("WebSocket-Verbindung geschlossen");
        }
        connected = false;
    }

    /**
     * Sendet eine Authentifizierungsanfrage an den Server
     *
     * @param username Benutzername
     * @param password Passwort
     * @param clientInfo Client-Informationen
     * @return CompletableFuture mit der Antwort des Servers
     */
    public CompletableFuture<WebSocketMessage> authenticate(String username, String password, String clientInfo) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new RuntimeException("Nicht mit Server verbunden"));
        }

        // Erstelle Authentifizierungsanfrage (DTO aus shared Modul)
        Object authRequest = createAuthenticationRequest(username, password, clientInfo);

        WebSocketMessage message = WebSocketMessage.builder()
                .type("authenticate")
                .requestId(UUID.randomUUID().toString())
                .data(authRequest)
                .timestamp(System.currentTimeMillis())
                .build();

        return webSocketClient.sendRequest(message).thenApply(response -> {
            // Setze Authentifizierungsstatus basierend auf Antwort
            if (response != null && response.getData() != null) {
                webSocketClient.setAuthenticated(true);
                log.info("Authentifizierung erfolgreich");
            }
            return response;
        });
    }

    /**
     * Sendet einen Funktionsaufruf an den Server
     *
     * @param functionName Name der aufzurufenden Funktion
     * @param parameters Parameter für den Funktionsaufruf
     * @return CompletableFuture mit der Antwort des Servers
     */
    public CompletableFuture<WebSocketMessage> callFunction(String functionName, Object parameters) {
        if (!isConnected()) {
            return CompletableFuture.failedFuture(new RuntimeException("Nicht mit Server verbunden"));
        }

        if (!webSocketClient.isAuthenticated()) {
            return CompletableFuture.failedFuture(new RuntimeException("Nicht authentifiziert"));
        }

        // Erstelle Funktionsaufruf-Request
        Object functionRequest = createFunctionCallRequest(functionName, parameters);

        WebSocketMessage message = WebSocketMessage.builder()
                .type("function_call")
                .requestId(UUID.randomUUID().toString())
                .data(functionRequest)
                .timestamp(System.currentTimeMillis())
                .build();

        return webSocketClient.sendRequest(message);
    }

    /**
     * Sendet eine Nachricht ohne auf Antwort zu warten
     *
     * @param type Nachrichtentyp
     * @param data Nutzdaten
     */
    public void sendMessage(String type, Object data) {
        if (!isConnected()) {
            log.warn("Kann Nachricht nicht senden - nicht verbunden");
            return;
        }

        WebSocketMessage message = WebSocketMessage.builder()
                .type(type)
                .data(data)
                .timestamp(System.currentTimeMillis())
                .build();

        webSocketClient.sendMessage(message);
    }

    /**
     * Registriert einen Handler für eingehende Nachrichten eines bestimmten Typs
     *
     * @param messageType Nachrichtentyp
     * @param handler Handler-Funktion
     */
    public void registerMessageHandler(String messageType, Consumer<WebSocketMessage> handler) {
        if (webSocketClient != null) {
            webSocketClient.registerMessageHandler(messageType, handler);
        }
    }

    /**
     * Entfernt einen Handler für einen bestimmten Nachrichtentyp
     *
     * @param messageType Nachrichtentyp
     */
    public void unregisterMessageHandler(String messageType) {
        if (webSocketClient != null) {
            webSocketClient.unregisterMessageHandler(messageType);
        }
    }

    /**
     * Prüft ob eine Verbindung zum Server besteht
     */
    public boolean isConnected() {
        return connected && webSocketClient != null && webSocketClient.isOpen();
    }

    /**
     * Prüft ob der Client authentifiziert ist
     */
    public boolean isAuthenticated() {
        return webSocketClient != null && webSocketClient.isAuthenticated();
    }

    /**
     * Erstellt eine Authentifizierungsanfrage
     * Verwendet ein einfaches Object anstatt der konkreten DTO-Klasse um Abhängigkeiten zu vermeiden
     */
    private Object createAuthenticationRequest(String usernameIn, String passwordIn, String clientInfoIn) {
        return new Object() {
            public final String username = usernameIn;
            public final String password = passwordIn;
            public final String clientInfo = clientInfoIn;
        };
    }

    /**
     * Erstellt eine Funktionsaufruf-Anfrage
     */
    private Object createFunctionCallRequest(String functionNameIn, Object parametersIn) {
        return new Object() {
            public final String functionName = functionNameIn;
            public final Object parameters = parametersIn;
        };
    }
}
