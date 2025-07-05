package de.mhus.nimbus.voxelworld.service;

import org.springframework.stereotype.Service;

/**
 * VoxelWorld Service
 * Geschäftslogik für VoxelWorld-Operationen
 */
@Service
public class VoxelWorldService {

    private static final int DEFAULT_WORLD_SIZE = 1000;
    private static final int DEFAULT_CHUNK_SIZE = 16;

    public String createWorld(String worldName) {
        // TODO: Implementierung der Welt-Erstellung
        return "World '" + worldName + "' created successfully";
    }

    public boolean isWorldActive(String worldName) {
        // TODO: Implementierung der Welt-Status-Prüfung
        return true;
    }

    public int getWorldSize() {
        return DEFAULT_WORLD_SIZE;
    }

    public int getChunkSize() {
        return DEFAULT_CHUNK_SIZE;
    }
}
