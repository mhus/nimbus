package de.mhus.nimbus.world.shared.world;

import de.mhus.nimbus.generated.types.ChunkData;
import de.mhus.nimbus.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class WChunkServiceTest {

    @Test
    void testInlineSaveAndLoad() {
        WChunkRepository repo = Mockito.mock(WChunkRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        WWorldService worldService = Mockito.mock(WWorldService.class);
        WItemRegistryService  itemRegistryService = Mockito.mock(WItemRegistryService.class);
        WChunkService service = new WChunkService(repo, storage, worldService, itemRegistryService);
        ChunkData cd = new ChunkData();
        cd.setCx(1); cd.setCz(2); cd.setSize((byte)16);
        WChunk chunk = WChunk.builder().regionId("r1").worldId("w1").chunk("c1").build();
        Mockito.when(repo.findByRegionIdAndWorldIdAndChunk("r1","w1","c1")).thenReturn(Optional.empty());
        Mockito.when(repo.save(Mockito.any())).thenAnswer(a -> a.getArgument(0));
        WChunk saved = service.saveChunk("r1","w1","c1", cd);
        assertNotNull(saved.getContent());
        assertNull(saved.getStorageId());
    }

    @Test
    void testExternalSave() {
        WChunkRepository repo = Mockito.mock(WChunkRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        WWorldService worldService = Mockito.mock(WWorldService.class);
        WItemRegistryService itemRegistry = Mockito.mock(WItemRegistryService.class);
        WChunkService service = new WChunkService(repo, storage, worldService, itemRegistry);
        service.setInlineMaxSize(10); // sehr klein um external zu erzwingen
        ChunkData cd = new ChunkData();
        cd.setCx(1); cd.setCz(2); cd.setSize((byte)16);
        cd.setBlocks(java.util.List.of());
        String largeJson = "{" + "\"cx\":1," + "\"cz\":2," + "\"size\":16," + "\"blocks\":[]}"; // > inlineMaxSize garantiert
        Mockito.when(repo.findByRegionIdAndWorldIdAndChunk("r1","w1","c2")).thenReturn(Optional.empty());
        Mockito.when(storage.store(Mockito.any(), Mockito.any())).thenReturn("STORAGE-ID");
        Mockito.when(repo.save(Mockito.any())).thenAnswer(a -> a.getArgument(0));
        WChunk saved = service.saveChunk("r1","w1","c2", cd);
        assertNull(saved.getContent());
//TODO        assertEquals("STORAGE-ID", saved.getStorageId());
    }
}
