package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.service.SchemaVersion;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WChunkServiceTest {

    @Test
    void testStreamSaveAndLoad() {
        WChunkRepository repo = Mockito.mock(WChunkRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        WWorldService worldService = Mockito.mock(WWorldService.class);
        WItemRegistryService itemRegistryService = Mockito.mock(WItemRegistryService.class);

        // Mock StorageService.store() um StorageInfo zurückzugeben
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STORAGE-ID", 100L, new java.util.Date(), "w1", "chunk/c1", "chunk", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        WChunkService service = new WChunkService(repo, storage, worldService, itemRegistryService);
        ChunkData cd = new ChunkData();
        cd.setCx(1);
        cd.setCz(2);
        cd.setSize((byte)16);

        Mockito.when(repo.findByRegionIdAndWorldIdAndChunk("r1","w1","c1")).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any())).thenAnswer(a -> a.getArgument(0));

        WChunk saved = service.saveChunk("r1","w1","c1", cd);

        // Alle Chunks sind jetzt extern gespeichert
        assertEquals("STORAGE-ID", saved.getStorageId());
        assertNotNull(saved.getStorageId());
    }

    @Test
    void testExternalSave() {
        WChunkRepository repo = Mockito.mock(WChunkRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        WWorldService worldService = Mockito.mock(WWorldService.class);
        WItemRegistryService itemRegistry = Mockito.mock(WItemRegistryService.class);

        // Mock StorageService.store() um StorageInfo zurückzugeben
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STORAGE-ID-2", 200L, new java.util.Date(), "w1", "chunk/c2", "chunk", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        WChunkService service = new WChunkService(repo, storage, worldService, itemRegistry);
        ChunkData cd = new ChunkData();
        cd.setCx(1);
        cd.setCz(2);
        cd.setSize((byte)16);
        cd.setBlocks(java.util.List.of());

        Mockito.when(repo.findByRegionIdAndWorldIdAndChunk("r1","w1","c2")).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any())).thenAnswer(a -> a.getArgument(0));

        WChunk saved = service.saveChunk("r1","w1","c2", cd);

        // Alle Chunks sind jetzt extern gespeichert
        assertEquals("STORAGE-ID-2", saved.getStorageId());
        assertNotNull(saved.getStorageId());
    }
}
