package de.mhus.nimbus.common.service;

import de.mhus.nimbus.common.client.IdentityClient;
import de.mhus.nimbus.common.client.RegistryClient;
import de.mhus.nimbus.common.client.WorldLifeClient;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class ClientMaintenanceService {

    private final IdentityClient identityClient;
    private final RegistryClient registryClient;
    private final WorldLifeClient worldLifeClient;

    @Autowired
    public ClientMaintenanceService(IdentityClient identityClient, RegistryClient registryClient, WorldLifeClient worldLifeClient) {
        this.identityClient = identityClient;
        this.registryClient = registryClient;
        this.worldLifeClient = worldLifeClient;
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
                log.debug("Starting identity client cleanup - Current pending requests: {}", identityClient.getPendingRequestStats());
                identityClient.cleanupExpiredRequests();
                int newIdentityPendingCount = identityClient.getPendingRequestCount();
                log.debug("Identity client cleanup completed - Remaining pending requests: {}", newIdentityPendingCount);
            }

            // RegistryClient cleanup
            int registryPendingCount = registryClient.getPendingRequestCount();
            if (registryPendingCount > 0) {
                log.debug("Starting registry client cleanup - Current pending requests: {}", registryClient.getPendingRequestStats());
                registryClient.cleanupExpiredRequests();
                int newRegistryPendingCount = registryClient.getPendingRequestCount();
                log.debug("Registry client cleanup completed - Remaining pending requests: {}", newRegistryPendingCount);
            }

            // WorldLifeClient cleanup
            int worldLifePendingCount = worldLifeClient.getPendingRequestCount();
            if (worldLifePendingCount > 0) {
                log.debug("Starting world-life client cleanup - Current pending requests: {}", worldLifeClient.getPendingRequestStats());
                worldLifeClient.cleanupExpiredRequests();
                int newWorldLifePendingCount = worldLifeClient.getPendingRequestCount();
                log.debug("World-life client cleanup completed - Remaining pending requests: {}", newWorldLifePendingCount);
            }

        } catch (Exception e) {
            log.error("Error during client cleanup", e);
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
            int worldLifePendingCount = worldLifeClient.getPendingRequestCount();

            if (identityPendingCount > 0 || registryPendingCount > 0 || worldLifePendingCount > 0) {
                log.info("Client status - Identity: {}, Registry: {}, WorldLife: {}",
                        identityClient.getPendingRequestStats(),
                        registryClient.getPendingRequestStats(),
                        worldLifeClient.getPendingRequestStats());
            }
        } catch (Exception e) {
            log.error("Error logging client statistics", e);
        }
    }

    /**
     * Gibt kombinierte Statistiken über alle Clients zurück
     */
    public String getCombinedClientStats() {
        return String.format("Client Stats - Identity: %s, Registry: %s, WorldLife: %s",
                identityClient.getPendingRequestStats(),
                registryClient.getPendingRequestStats(),
                worldLifeClient.getPendingRequestStats());
    }

    /**
     * Gibt die Gesamtanzahl aller wartenden Requests zurück
     */
    public int getTotalPendingRequestCount() {
        return identityClient.getPendingRequestCount() +
                registryClient.getPendingRequestCount() +
                worldLifeClient.getPendingRequestCount();
    }
}
