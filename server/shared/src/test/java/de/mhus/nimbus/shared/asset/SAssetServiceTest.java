package de.mhus.nimbus.shared.asset;

import de.mhus.nimbus.shared.persistence.SAsset;
import de.mhus.nimbus.shared.persistence.SAssetRepository;
import de.mhus.nimbus.shared.storage.StorageService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SAssetServiceTest {

    @Test
    void testInlineSave() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        SAssetService service = new SAssetService(repo, Optional.of(storage));
        byte[] data = new byte[100];
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        SAsset asset = service.saveAsset("r1", null, "folder/test.txt", data, "tester");
        assertTrue(asset.isInline());
        assertEquals("test.txt", asset.getName());
        assertEquals(100, asset.getSize());
    }

    @Test
    void testExternalSave() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        Mockito.when(storage.store(Mockito.any(), Mockito.any())).thenReturn("STOR-1");
        SAssetService service = new SAssetService(repo, Optional.of(storage));
        // Setze inline Grenze sehr klein damit external greift
        byte[] big = new byte[200];
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        assertTrue(service.getInlineMaxSize() >= 3_000_000); // standard
        service.setInlineMaxSize(100);
        // Simuliere Konfigurationsänderung
        // Erzwinge extern indem wir die Grenze manuell per Reflection reduzieren (optional) -> überspringen, nutzen vorhandenen Wert und großes Array
        SAsset asset = service.saveAsset("r1", "w1", "folder/big.bin", big, "tester");
        assertTrue(asset.isStoredExternal());
        assertEquals("STOR-1", asset.getStorageId());
        assertNull(asset.getContent());
    }

    @Test
    void testUpdateToExternal() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        Mockito.when(storage.store(Mockito.any(), Mockito.any())).thenReturn("STOR-NEW");
        SAssetService service = new SAssetService(repo, Optional.of(storage));
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        SAsset inline = service.saveAsset("r1", null, "file.bin", new byte[100], "tester");
        assertTrue(inline.isInline());
        // Update mit großem Inhalt
        Optional<SAsset> updated = service.updateContent(inline.getId(), new byte[5000000]);
        // Da Repo.findById nicht gemockt ist, updated leer -> wir mocken findById jetzt
        Mockito.when(repo.findById(inline.getId())).thenReturn(Optional.of(inline));
        updated = service.updateContent(inline.getId(), new byte[5000000]);
        assertTrue(updated.isPresent());
        assertTrue(updated.get().isStoredExternal());
        assertEquals("STOR-NEW", updated.get().getStorageId());
    }

    @Test
    void testDisable() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);
        SAssetService service = new SAssetService(repo, Optional.of(storage));
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));
        SAsset asset = service.saveAsset("r1", null, "folder/test.txt", new byte[10], "tester");
        Mockito.when(repo.findById(asset.getId())).thenReturn(Optional.of(asset));
        service.disable(asset.getId());
        assertFalse(asset.isEnabled());
        // loadContent sollte jetzt Exception werfen
        assertThrows(IllegalStateException.class, () -> service.loadContent(asset));
    }
}
