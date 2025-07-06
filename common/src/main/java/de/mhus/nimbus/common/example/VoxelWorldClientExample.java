package de.mhus.nimbus.common.example;

import de.mhus.nimbus.common.client.WorldVoxelClient;
import de.mhus.nimbus.shared.voxel.Voxel;
import de.mhus.nimbus.shared.voxel.VoxelChunk;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Beispiel-Service, der zeigt, wie der WorldVoxelClient verwendet wird
 */
@Service
public class VoxelWorldClientExample {

    private static final Logger LOGGER = LoggerFactory.getLogger(VoxelWorldClientExample.class);

    private final WorldVoxelClient worldVoxelClient;

    @Autowired
    public VoxelWorldClientExample(WorldVoxelClient worldVoxelClient) {
        this.worldVoxelClient = worldVoxelClient;
    }

    /**
     * Beispiel: Speichert einen Voxel asynchron
     */
    public CompletableFuture<Void> saveVoxelExample(String worldId, Voxel voxel) {
        LOGGER.info("Speichere Voxel an Position ({}, {}, {}) in Welt {}",
                   voxel.getX(), voxel.getY(), voxel.getZ(), worldId);

        return worldVoxelClient.saveVoxel(worldId, voxel)
            .thenRun(() -> LOGGER.info("Voxel erfolgreich gespeichert"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Speichern des Voxels: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Löscht einen Voxel
     */
    public CompletableFuture<Void> deleteVoxelExample(String worldId, int x, int y, int z) {
        LOGGER.info("Lösche Voxel an Position ({}, {}, {}) in Welt {}", x, y, z, worldId);

        return worldVoxelClient.deleteVoxel(worldId, x, y, z)
            .thenRun(() -> LOGGER.info("Voxel erfolgreich gelöscht"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Löschen des Voxels: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Speichert mehrere Voxel in einem Batch
     */
    public CompletableFuture<Void> batchSaveExample(String worldId, List<Voxel> voxels) {
        LOGGER.info("Speichere {} Voxel in Batch für Welt {}", voxels.size(), worldId);

        return worldVoxelClient.batchSaveVoxels(worldId, voxels)
            .thenRun(() -> LOGGER.info("Batch-Speicherung erfolgreich abgeschlossen"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler bei Batch-Speicherung: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Speichert einen kompletten Chunk
     */
    public CompletableFuture<Void> saveChunkExample(String worldId, VoxelChunk chunk) {
        LOGGER.info("Speichere Chunk ({}, {}, {}) in Welt {}",
                   chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ(), worldId);

        return worldVoxelClient.saveChunk(worldId, chunk)
            .thenRun(() -> LOGGER.info("Chunk erfolgreich gespeichert"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Speichern des Chunks: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Lädt einen Chunk mit leeren Voxeln
     */
    public CompletableFuture<Void> loadFullChunkExample(String worldId, int chunkX, int chunkY, int chunkZ) {
        LOGGER.info("Lade vollständigen Chunk ({}, {}, {}) aus Welt {}", chunkX, chunkY, chunkZ, worldId);

        return worldVoxelClient.loadFullChunk(worldId, chunkX, chunkY, chunkZ, true)
            .thenRun(() -> LOGGER.info("Chunk-Ladeanfrage erfolgreich gesendet"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Laden des Chunks: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Löscht alle Voxel in einem Chunk
     */
    public CompletableFuture<Void> clearChunkExample(String worldId, int chunkX, int chunkY, int chunkZ) {
        LOGGER.info("Lösche alle Voxel in Chunk ({}, {}, {}) in Welt {}", chunkX, chunkY, chunkZ, worldId);

        return worldVoxelClient.clearChunk(worldId, chunkX, chunkY, chunkZ)
            .thenRun(() -> LOGGER.info("Chunk erfolgreich geleert"))
            .exceptionally(throwable -> {
                LOGGER.error("Fehler beim Leeren des Chunks: {}", throwable.getMessage());
                return null;
            });
    }

    /**
     * Beispiel: Kombinierte Operation - speichere und lade dann einen Chunk
     */
    public CompletableFuture<Void> saveAndLoadChunkExample(String worldId, VoxelChunk chunk) {
        return saveChunkExample(worldId, chunk)
            .thenCompose(result -> {
                LOGGER.info("Chunk gespeichert, lade ihn nun wieder...");
                return loadFullChunkExample(worldId, chunk.getChunkX(), chunk.getChunkY(), chunk.getChunkZ());
            })
            .thenRun(() -> LOGGER.info("Speicher- und Ladevorgang abgeschlossen"));
    }
}
