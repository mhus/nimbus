package de.mhus.nimbus.shared.asset;

import de.mhus.nimbus.shared.persistence.SAsset;
import de.mhus.nimbus.shared.persistence.SAssetRepository;
import de.mhus.nimbus.shared.storage.StorageService;
import de.mhus.nimbus.shared.service.SchemaVersion;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SAssetServiceTest {

    @Test
    void testStreamSave() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);

        // Mock StorageService.store() um StorageInfo zurückzugeben
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STOR-123", 100L, new Date(), "w1", "assets/folder/test.txt", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        SAssetService service = new SAssetService(repo, storage);
        byte[] data = new byte[100];
        InputStream stream = new ByteArrayInputStream(data);

        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SAsset asset = service.saveAsset("r1", null, "folder/test.txt", stream, "tester");

        // Alle Assets sind jetzt extern gespeichert
        assertEquals("STOR-123", asset.getStorageId());
        assertEquals("test.txt", asset.getName());
        assertEquals(100, asset.getSize());
    }

    @Test
    void testExternalSave() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);

        // Mock StorageService.store() um StorageInfo zurückzugeben
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STOR-BIG", 200L, new Date(), "w1", "assets/folder/big.bin", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        SAssetService service = new SAssetService(repo, storage);
        byte[] big = new byte[200];
        InputStream stream = new ByteArrayInputStream(big);

        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        SAsset asset = service.saveAsset("r1", "w1", "folder/big.bin", stream, "tester");

        // Alle Assets sind jetzt extern gespeichert
        assertEquals("STOR-BIG", asset.getStorageId());
        assertEquals(200, asset.getSize());
        assertNotNull(asset.getStorageId()); // Hat externe Storage-ID
    }

    @Test
    void testUpdateContent() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);

        // Mock für initial save
        StorageService.StorageInfo initialInfo = new StorageService.StorageInfo("STOR-INIT", 100L, new Date(), "w1", "assets/file.bin", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(initialInfo);

        // Mock für update
        StorageService.StorageInfo updateInfo = new StorageService.StorageInfo("STOR-UPDATED", 5000000L, new Date(), "w1", "assets/file.bin", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.update(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(updateInfo);

        SAssetService service = new SAssetService(repo, storage);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        // Initial Asset erstellen
        InputStream initialStream = new ByteArrayInputStream(new byte[100]);
        SAsset asset = service.saveAsset("r1", null, "file.bin", initialStream, "tester");
        assertEquals("STOR-INIT", asset.getStorageId());

        // Update mit größerem Content
        Mockito.when(repo.findById(asset.getId())).thenReturn(Optional.of(asset));
        InputStream updateStream = new ByteArrayInputStream(new byte[5000000]);
        SAsset updated = service.updateContent(asset.getId(), updateStream);

        assertNotNull(updated);
        assertEquals("STOR-UPDATED", updated.getStorageId());
        assertEquals(5000000, updated.getSize());
    }

    @Test
    void testDisable() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);

        // Mock StorageService.store() um StorageInfo zurückzugeben
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STOR-DISABLE", 10L, new Date(), "w1", "assets/folder/test.txt", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        SAssetService service = new SAssetService(repo, storage);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        InputStream stream = new ByteArrayInputStream(new byte[10]);
        SAsset asset = service.saveAsset("r1", null, "folder/test.txt", stream, "tester");

        Mockito.when(repo.findById(asset.getId())).thenReturn(Optional.of(asset));
        service.disable(asset.getId());

        assertFalse(asset.isEnabled());
        // loadContent sollte jetzt Exception werfen
        assertThrows(IllegalStateException.class, () -> service.loadContent(asset));
    }

    @Test
    void testLoadContentStream() {
        SAssetRepository repo = Mockito.mock(SAssetRepository.class);
        StorageService storage = Mockito.mock(StorageService.class);

        // Mock für StorageService.store()
        StorageService.StorageInfo storageInfo = new StorageService.StorageInfo("STOR-LOAD", 50L, new Date(), "w1", "assets/test.json", "asset", SchemaVersion.of("1.0"));
        Mockito.when(storage.store(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(storageInfo);

        // Mock für StorageService.load() - gibt InputStream zurück
        byte[] testData = "test content".getBytes();
        InputStream mockInputStream = new ByteArrayInputStream(testData);
        Mockito.when(storage.load("STOR-LOAD")).thenReturn(mockInputStream);

        SAssetService service = new SAssetService(repo, storage);
        Mockito.when(repo.save(Mockito.any())).thenAnswer(inv -> inv.getArgument(0));

        // Asset erstellen
        InputStream saveStream = new ByteArrayInputStream(testData);
        SAsset asset = service.saveAsset("r1", "w1", "test.json", saveStream, "tester");
        assertEquals("STOR-LOAD", asset.getStorageId());

        // Asset laden und Stream-API testen
        try (InputStream loadedStream = service.loadContent(asset)) {
            assertNotNull(loadedStream);
            // Der Mock-InputStream sollte verfügbar sein
            assertTrue(loadedStream.available() >= 0);
        } catch (Exception e) {
            fail("Stream sollte lesbar sein: " + e.getMessage());
        }
    }
}
