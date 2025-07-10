package de.mhus.nimbus.client.common.example;

import de.mhus.nimbus.client.common.config.NimbusClientBuilder;
import de.mhus.nimbus.client.common.service.NimbusClientService;
import de.mhus.nimbus.shared.dto.WebSocketMessage;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;

/**
 * Beispiel für die Verwendung des NimbusClientService
 */
@Slf4j
public class ClientExample {

    public static void main(String[] args) {
        // Beispiel 1: Einfache Verwendung
        simpleExample();

        // Beispiel 2: Mit Builder
        builderExample();
    }

    /**
     * Einfaches Beispiel für die direkte Verwendung des NimbusClientService
     */
    public static void simpleExample() {
        NimbusClientService clientService = new NimbusClientService();

        // Registriere Handler für verschiedene Nachrichtentypen
        clientService.registerMessageHandler("notification", message -> {
            LOGGER.info("Benachrichtigung erhalten: {}", message.getData());
        });

        clientService.registerMessageHandler("broadcast", message -> {
            LOGGER.info("Broadcast erhalten: {}", message.getData());
        });

        // Verbinde mit Server
        clientService.connect("ws://localhost:8080/nimbus")
            .thenCompose(v -> {
                LOGGER.info("Verbindung hergestellt, authentifiziere...");
                return clientService.authenticate("testuser", "password", "JavaClient/1.0");
            })
            .thenCompose(authResponse -> {
                LOGGER.info("Authentifizierung erfolgreich: {}", authResponse);
                // Rufe eine Funktion auf dem Server auf
                return clientService.callFunction("getUserProfile", null);
            })
            .thenAccept(response -> {
                LOGGER.info("Funktionsaufruf-Antwort: {}", response);
            })
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Client-Betrieb", throwable);
                return null;
            })
            .whenComplete((result, throwable) -> {
                // Verbindung schließen
                clientService.disconnect();
            });
    }

    /**
     * Beispiel mit Builder-Pattern für erweiterte Konfiguration
     */
    public static void builderExample() {
        CompletableFuture<NimbusClientService> clientFuture = NimbusClientBuilder.create()
            .serverUrl("ws://localhost:8080/nimbus")
            .defaultMessageHandler(message -> {
                LOGGER.info("Standard-Handler: Nachricht erhalten vom Typ '{}': {}",
                    message.getType(), message.getData());
            })
            .autoReconnect(true)
            .reconnectInterval(10)
            .buildAndConnect();

        clientFuture
            .thenCompose(client -> {
                LOGGER.info("Client verbunden und bereit");

                // Authentifiziere
                return client.authenticate("admin", "secret", "AdminClient/1.0")
                    .thenApply(authResponse -> client);
            })
            .thenAccept(client -> {
                // Sende eine einfache Nachricht
                client.sendMessage("ping", "Hello Server!");

                // Rufe verschiedene Funktionen auf
                client.callFunction("getServerStatus", null)
                    .thenAccept(response -> LOGGER.info("Server Status: {}", response));

                client.callFunction("listWorlds", null)
                    .thenAccept(response -> LOGGER.info("Verfügbare Welten: {}", response));
            })
            .exceptionally(throwable -> {
                LOGGER.error("Fehler im Builder-Beispiel", throwable);
                return null;
            });
    }
}
