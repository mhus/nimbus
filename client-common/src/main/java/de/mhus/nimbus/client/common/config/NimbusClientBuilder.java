package de.mhus.nimbus.client.common.config;

import de.mhus.nimbus.client.common.service.NimbusClientService;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * Builder-Klasse für die einfache Konfiguration und Erstellung von NimbusClientService Instanzen
 */
@Slf4j
public class NimbusClientBuilder {

    private String serverUrl;
    private Consumer<WebSocketMessage> defaultMessageHandler;
    private boolean autoReconnect = false;
    private int reconnectIntervalSeconds = 5;

    private NimbusClientBuilder() {}

    /**
     * Erstellt einen neuen Builder
     */
    public static NimbusClientBuilder create() {
        return new NimbusClientBuilder();
    }

    /**
     * Setzt die Server-URL
     */
    public NimbusClientBuilder serverUrl(String serverUrl) {
        this.serverUrl = serverUrl;
        return this;
    }

    /**
     * Setzt einen Standard-Handler für alle eingehenden Nachrichten
     */
    public NimbusClientBuilder defaultMessageHandler(Consumer<WebSocketMessage> handler) {
        this.defaultMessageHandler = handler;
        return this;
    }

    /**
     * Aktiviert automatische Wiederverbindung (noch nicht implementiert)
     */
    public NimbusClientBuilder autoReconnect(boolean autoReconnect) {
        this.autoReconnect = autoReconnect;
        return this;
    }

    /**
     * Setzt das Intervall für automatische Wiederverbindungsversuche
     */
    public NimbusClientBuilder reconnectInterval(int seconds) {
        this.reconnectIntervalSeconds = seconds;
        return this;
    }

    /**
     * Erstellt und konfiguriert den NimbusClientService
     */
    public NimbusClientService build() {
        if (serverUrl == null || serverUrl.trim().isEmpty()) {
            throw new IllegalArgumentException("Server-URL muss gesetzt sein");
        }

        NimbusClientService clientService = new NimbusClientService();

        if (defaultMessageHandler != null) {
            // Registriere Default-Handler für alle Nachrichtentypen
            clientService.registerMessageHandler("*", defaultMessageHandler);
        }

        log.info("NimbusClientService konfiguriert für Server: {}", serverUrl);
        return clientService;
    }

    /**
     * Erstellt, konfiguriert und verbindet den NimbusClientService
     */
    public CompletableFuture<NimbusClientService> buildAndConnect() {
        NimbusClientService clientService = build();

        return clientService.connect(serverUrl)
                .thenApply(v -> {
                    log.info("Client erfolgreich verbunden mit {}", serverUrl);
                    return clientService;
                })
                .exceptionally(throwable -> {
                    log.error("Fehler beim Verbinden mit {}", serverUrl, throwable);
                    throw new RuntimeException("Verbindung fehlgeschlagen", throwable);
                });
    }
}
