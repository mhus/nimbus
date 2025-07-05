package de.mhus.nimbus.voxelworld.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * VoxelWorld Konfiguration
 * Konfigurationsparameter für das VoxelWorld-Modul
 */
@Configuration
@ConfigurationProperties(prefix = "voxelworld")
public class VoxelWorldConfig {

    private int worldSize = 1000;
    private int chunkSize = 16;
    private int maxPlayers = 100;

    public int getWorldSize() {
        return worldSize;
    }

    public void setWorldSize(int worldSize) {
        this.worldSize = worldSize;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public void setMaxPlayers(int maxPlayers) {
        this.maxPlayers = maxPlayers;
    }
}
