package de.mhus.nimbus.voxelworld.config;

import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VoxelWorld Konfiguration
 * Konfigurationsparameter für das VoxelWorld-Modul
 */
@Slf4j
@Data
@Configuration
@ConfigurationProperties(prefix = "voxelworld")
public class VoxelWorldConfig {

    private int worldSize = 1000;
    private int chunkSize = 16;
    private int maxPlayers = 100;

    @PostConstruct
    public void logConfiguration() {
        log.info("VoxelWorld Configuration loaded:");
        log.info("  - World Size: {}", worldSize);
        log.info("  - Chunk Size: {}", chunkSize);
        log.info("  - Max Players: {}", maxPlayers);
    }
}
