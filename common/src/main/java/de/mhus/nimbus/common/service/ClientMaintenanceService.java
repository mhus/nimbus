package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.common.client.RegistryClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Service für die Verwaltung der Client-Komponenten
 * Führt periodische Wartungsaufgaben aus
 */
@Service
public class ClientMaintenanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ClientMaintenanceService.class);

    private final IdentityClient identityClient;
    private final RegistryClient registryClient;

    @Autowired
    public ClientMaintenanceService(IdentityClient identityClient, RegistryClient registryClient) {
        this.identityClient = identityClient;
        this.registryClient = registryClient;
    }

    /**
     * Bereinigt alle 5 Minuten abgelaufene Requests
     */
    @Scheduled(fixedRate = 300000) // 5 Minuten in Millisekunden
    public void cleanupExpiredRequests() {
        try {
            // IdentityClient cleanup
            int identityPendingCount = identityClient.getPendingRequestCount();
            if (identityPendingCount > 0) {
                LOGGER.debug("Starting identity client cleanup - Current pending requests: {}", identityClient.getPendingRequestStats());
                identityClient.cleanupExpiredRequests();
                int newIdentityPendingCount = identityClient.getPendingRequestCount();
                LOGGER.debug("Identity client cleanup completed - Remaining pending requests: {}", newIdentityPendingCount);
            }

            // RegistryClient cleanup
            int registryPendingCount = registryClient.getPendingRequestCount();
            if (registryPendingCount > 0) {
                LOGGER.debug("Starting registry client cleanup - Current pending requests: {}", registryClient.getPendingRequestStats());
                registryClient.cleanupExpiredRequests();
                int newRegistryPendingCount = registryClient.getPendingRequestCount();
                LOGGER.debug("Registry client cleanup completed - Remaining pending requests: {}", newRegistryPendingCount);
            }

        } catch (Exception e) {
            LOGGER.error("Error during client cleanup", e);
        }
    }

    /**
     * Loggt alle 10 Minuten Statistiken über wartende Requests
     */
    @Scheduled(fixedRate = 600000) // 10 Minuten in Millisekunden
    public void logPendingRequestStats() {
        try {
            int identityPendingCount = identityClient.getPendingRequestCount();
            int registryPendingCount = registryClient.getPendingRequestCount();

            if (identityPendingCount > 0 || registryPendingCount > 0) {
                LOGGER.info("Client status - Identity: {}, Registry: {}",
                           identityClient.getPendingRequestStats(),
                           registryClient.getPendingRequestStats());
            }
        } catch (Exception e) {
            LOGGER.error("Error logging client statistics", e);
        }
    }

    /**
     * Gibt kombinierte Statistiken über alle Clients zurück
     */
    public String getCombinedClientStats() {
        return String.format("Client Stats - Identity: %s, Registry: %s",
                           identityClient.getPendingRequestStats(),
                           registryClient.getPendingRequestStats());
    }

    /**
     * Gibt die Gesamtanzahl aller wartenden Requests zurück
     */
    public int getTotalPendingRequestCount() {
        return identityClient.getPendingRequestCount() + registryClient.getPendingRequestCount();
    }
}
