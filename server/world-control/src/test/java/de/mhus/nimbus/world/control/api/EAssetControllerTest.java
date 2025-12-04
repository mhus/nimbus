package de.mhus.nimbus.world.control.api;

import de.mhus.nimbus.shared.asset.SAssetService;
import de.mhus.nimbus.shared.persistence.SAsset;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EAssetControllerTest {

    @Test
    void testGetNotFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        Mockito.when(service.findByPath("reg1", "world1", "path/file.txt")).thenReturn(Optional.empty());
        EAssetController ctrl = new EAssetController(service);
        ResponseEntity<?> resp = ctrl.get("reg1", "world1", "path/file.txt", false);
        assertEquals(404, resp.getStatusCodeValue());
    }

    @Test
    void testGetFound() {
        SAssetService service = Mockito.mock(SAssetService.class);
        SAsset asset = new SAsset();
        asset.setId("X1");
        asset.setRegionId("reg1");
        asset.setWorldId("world1");
        asset.setPath("path/file.txt");
        asset.setName("file.txt");
        asset.setCreatedAt(Instant.now());
        asset.setCreatedBy("editor");
        asset.setEnabled(true);
        Mockito.when(service.findByPath("reg1", "world1", "path/file.txt")).thenReturn(Optional.of(asset));
        EAssetController ctrl = new EAssetController(service);
        ResponseEntity<?> resp = ctrl.get("reg1", "world1", "path/file.txt", false);
        assertEquals(200, resp.getStatusCodeValue());
    }
}

