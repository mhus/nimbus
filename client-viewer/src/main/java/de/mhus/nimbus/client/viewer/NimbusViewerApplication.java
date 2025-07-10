package de.mhus.nimbus.client.viewer;

import de.mhus.nimbus.client.common.service.NimbusClientService;
import lombok.extern.slf4j.Slf4j;

/**
 * Hauptanwendungsklasse für den Nimbus Client Viewer
 * Startet das grafische Frontend und initialisiert die Verbindung zum Server
 */
@Slf4j
public class NimbusViewerApplication {

    private static final String DEFAULT_SERVER_URL = "ws://localhost:8080/nimbus";

    public static void main(String[] args) {
        log.info("Starte Nimbus Client Viewer...");

        try {
            // Erstelle Client-Service
            NimbusClientService clientService = new NimbusClientService();

            // Erstelle und starte die grafische Benutzeroberfläche
            ViewerWindow viewerWindow = new ViewerWindow(clientService);
            viewerWindow.run();

        } catch (Exception e) {
            log.error("Fehler beim Starten des Nimbus Client Viewers", e);
            System.exit(1);
        }
    }
}
